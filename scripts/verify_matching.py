#!/usr/bin/env python3
"""
verify_matching.py - Verify reconcileIQ matching against demo expected outcomes

Logs in as exec@demo.com via the API, uploads demo files, runs matching,
and compares results with expected_outcomes_demo.csv.
"""

import csv
import hashlib
import json
import os
import subprocess
import sys
import time
from datetime import date

import requests

# Configuration
BASE_URL = "http://localhost:8080/api/v1"

# Use demo files for verification
ORDERS_CSV = "/home/dev-akash/isocode/run1/orders_demo.csv"
SETTLEMENTS_CSV = "/home/dev-akash/isocode/run1/settlements_demo.csv"
COD_REMITTANCES_CSV = "/home/dev-akash/isocode/run1/cod_remittances_demo.csv"
FREIGHT_INVOICES_CSV = "/home/dev-akash/isocode/run1/freight_invoices_demo.csv"
DEMO_DEMO_CSV = "/home/dev-akash/isocode/expected_outcomes_demo.csv"

USERNAME = "exec@demo.com"
PASSWORD = "Exec@12345"


def login():
    """Login and get auth token."""
    resp = requests.post(f"{BASE_URL}/auth/login", json={"email": USERNAME, "password": PASSWORD})
    resp.raise_for_status()
    token = resp.json()["token"]
    return token


def upload_file(token, file_path, source_id, file_type):
    """Upload a file and return batch info."""
    with open(file_path, "rb") as f:
        files = {"file": (os.path.basename(file_path), f, "text/csv")}
        data = {"sourceId": str(source_id), "fileType": file_type}
        headers = {"Authorization": f"Bearer {token}"}
        resp = requests.post(f"{BASE_URL}/uploads", files=files, data=data, headers=headers)
    resp.raise_for_status()
    return resp.json()


def ingest_batch(token, batch_id, file_path, source_id, file_type):
    """Ingest a previously uploaded file."""
    with open(file_path, "rb") as f:
        files = {"file": (os.path.basename(file_path), f, "text/csv")}
        data = {"sourceId": str(source_id), "fileType": file_type}
        headers = {"Authorization": f"Bearer {token}"}
        resp = requests.post(f"{BASE_URL}/uploads/{batch_id}/ingest", files=files, data=data, headers=headers)
    resp.raise_for_status()
    return resp.json()


def run_matching(token, as_of_date="2026-11-06"):
    """Run matching with the given asOfDate."""
    headers = {"Authorization": f"Bearer {token}"}
    resp = requests.post(f"{BASE_URL}/matching/run", params={"asOfDate": as_of_date}, headers=headers)
    resp.raise_for_status()
    return resp.json()


def get_order_reconciliations(token):
    """Get all order reconciliations from the database via psql."""
    # Use docker compose exec to run psql query
    result = subprocess.run(
        ["docker", "compose", "exec", "-T", "postgres", "psql", "-U", "reconcileiq", "-d", "reconcileiq",
         "-c", "SELECT order_id, expected_settlement, received_settlement, settlement_diff, reconciliation_status, money_at_risk FROM order_reconciliation ORDER BY order_id;"],
        capture_output=True, text=True
    )
    if result.returncode != 0:
        print(f"psql error: {result.stderr}", file=sys.stderr)
        return []

    lines = result.stdout.strip().split("\n")
    headers_line = lines[0]
    rows = []
    for line in lines[1:]:
        parts = line.split("|")
        if len(parts) >= 6:
            rows.append({
                "order_id": parts[0],
                "expected_settlement": parts[1],
                "received_settlement": parts[2],
                "settlement_diff": parts[3],
                "reconciliation_status": parts[4],
                "money_at_risk": parts[5],
            })
    return rows


def load_demo_expected():
    """Load expected outcomes from demo CSV."""
    expected = {}
    with open(DEMO_DEMO_CSV, "r") as f:
        reader = csv.DictReader(f)
        for row in reader:
            expected[row["order_id"]] = {
                "expected_settlement": row["expected_settlement"],
                "received_settlement": row["received_settlement"],
                "settlement_diff": row["difference"],
                "reconciliation_status": row["status"],
                "money_at_risk": row["money_at_risk"],
            }
    return expected


def main():
    print("=" * 60)
    print("ReconcileIQ Matching Verification Script")
    print("=" * 60)

    # Step 1: Login
    print("\n[1] Logging in as exec@demo.com...")
    token = login()
    print(f"    Login successful, token acquired")

    # Step 2: Upload demo order file (source_id 1 = Website)
    print("\n[2] Uploading demo orders file...")
    orders_batch = upload_file(token, ORDERS_CSV, 1, "order")
    print(f"    Orders batch ID: {orders_batch['batchId']}, valid rows: {orders_batch['validRows']}")

    # Step 3: Upload demo settlement file
    print("\n[3] Uploading demo settlement file...")
    settle_batch = upload_file(token, SETTLEMENTS_CSV, 1, "settlement")
    print(f"    Settlement batch ID: {settle_batch['batchId']}, valid rows: {settle_batch['validRows']}")

    print("\n[4] Uploading demo COD remittance file...")
    cod_batch = upload_file(token, COD_REMITTANCES_CSV, 1, "cod_remittance")
    print(f"    COD batch ID: {cod_batch['batchId']}, valid rows: {cod_batch['validRows']}")

    print("\n[5] Uploading demo freight invoice file...")
    freight_batch = upload_file(token, FREIGHT_INVOICES_CSV, 1, "freight_invoice")
    print(f"    Freight batch ID: {freight_batch['batchId']}, valid rows: {freight_batch['validRows']}")

    # Step 4: Ingest the files
    print("\n[6] Ingesting orders file...")
    ingest_batch(token, orders_batch["batchId"], ORDERS_CSV, 1, "order")

    print("\n[7] Ingesting settlement file...")
    ingest_batch(token, settle_batch["batchId"], SETTLEMENTS_CSV, 1, "settlement")

    print("\n[8] Ingesting COD remittance file...")
    ingest_batch(token, cod_batch["batchId"], COD_REMITTANCES_CSV, 1, "cod_remittance")

    print("\n[9] Ingesting freight invoice file...")
    ingest_batch(token, freight_batch["batchId"], FREIGHT_INVOICES_CSV, 1, "freight_invoice")

    # Step 5: Run matching
    print("\n[10] Running matching with asOfDate=2026-11-06...")
    summary = run_matching(token, "2026-11-06")
    print(f"    Total orders: {summary.get('totalOrders', 'N/A')}")
    print(f"    UNPAID_PAST_DUE: {summary.get('unpaidPastDue', 'N/A')}")
    print(f"    SHORT_PAID: {summary.get('shortPaid', 'N/A')}")
    print(f"    FREIGHT_OVERCHARGED: {summary.get('freightOvercharged', 'N/A')}")
    print(f"    SETTLED: {summary.get('settled', 'N/A')}")
    print(f"    PENDING: {summary.get('pending', 'N/A')}")
    print(f"    Money at risk: {summary.get('moneyAtRisk', 'N/A')}")
    print(f"    Auto match rate: {summary.get('autoMatchRate', 'N/A')}")

    # Step 5: Get results from database
    print("\n[11] Reading order reconciliations from database...")
    reconciliations = get_order_reconciliations(token)

    # Step 6: Compare with expected outcomes
    print("\n[12] Comparing with expected_outcomes_demo.csv...")
    expected = load_demo_expected()

    compared = 0
    mismatches = 0
    mismatch_rows = []

    for recon in reconciliations:
        order_id = recon["order_id"]
        if order_id not in expected:
            continue

        compared += 1
        exp = expected[order_id]

        actual_expected = str(recon["expected_settlement"])
        actual_received = str(recon["received_settlement"])
        actual_diff = str(recon["settlement_diff"])
        actual_status = recon["reconciliation_status"]
        actual_mar = str(recon["money_at_risk"])

        # Check each field
        row_mismatches = []

        if actual_expected != exp["expected_settlement"]:
            row_mismatches.append(
                f"expected_set: expected={exp['expected_settlement']} actual={actual_expected}"
            )

        if actual_received != exp["received_settlement"]:
            row_mismatches.append(
                f"received_set: expected={exp['received_settlement']} actual={actual_received}"
            )

        if actual_diff != exp["settlement_diff"]:
            row_mismatches.append(
                f"diff: expected={exp['settlement_diff']} actual={actual_diff}"
            )

        if actual_status != exp["reconciliation_status"]:
            row_mismatches.append(
                f"status: expected={exp['reconciliation_status']} actual={actual_status}"
            )

        if actual_mar != exp["money_at_risk"]:
            row_mismatches.append(
                f"money_at_risk: expected={exp['money_at_risk']} actual={actual_mar}"
            )

        if row_mismatches:
            mismatches += 1
            mismatch_rows.append({
                "order_id": order_id,
                "expected": exp,
                "actual": {
                    "expected_settlement": actual_expected,
                    "received_settlement": actual_received,
                    "settlement_diff": actual_diff,
                    "reconciliation_status": actual_status,
                    "money_at_risk": actual_mar,
                }
            })
            print(f"    MISMATCH {order_id}: {'; '.join(row_mismatches)}")
        else:
            print(f"    OK {order_id}: all fields match")

    # Print summary
    print("\n" + "=" * 60)
    print(f"Summary: compared {compared}, mismatches {mismatches}")
    print("=" * 60)

    if mismatches > 0:
        print(f"\nFirst 10 mismatching rows:")
        for mr in mismatch_rows[:10]:
            print(f"\n  Order: {mr['order_id']}")
            print(f"    Expected: expected_settlement={mr['expected']['expected_settlement']}, "
                  f"received_settlement={mr['expected']['received_settlement']}, "
                  f"diff={mr['expected']['settlement_diff']}, "
                  f"status={mr['expected']['reconciliation_status']}, "
                  f"money_at_risk={mr['expected']['money_at_risk']}")
            print(f"    Actual: expected_settlement={mr['actual']['expected_settlement']}, "
                  f"received_settlement={mr['actual']['received_settlement']}, "
                  f"diff={mr['actual']['settlement_diff']}, "
                  f"status={mr['actual']['reconciliation_status']}, "
                  f"money_at_risk={mr['actual']['money_at_risk']}")

    # Exit with code 0 (we just want to print output)
    sys.exit(0)


if __name__ == "__main__":
    main()