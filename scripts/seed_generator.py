#!/usr/bin/env python3
"""
Deterministic Seed Generator for ReconcileIQ Phase 3

- 3 brief rows as fixtures (exact brief match)
- 100k orders: 5 channels, 2 couriers, 55% COD on Website
- Realistic anomaly rates: not_yet_due ~5%, total flagged ~5-8%, rest SETTLED
- Full-size partner files: settlements ~95k+, COD ~55k, freight ~95k
- Different column names per partner file
- Deterministic: run twice, hash ALL output CSVs, print sha256 side by side
- Status distribution + wc -l for all files
"""

import csv
import hashlib
import random
import os
import shutil
from datetime import date, timedelta

RANDOM_SEED = 42
TOLERANCE = 10.00

# ── 1) Brief fixture rows (MUST match brief exactly) ──────────────────
BRIEF_FIXTURES = [
    ["ORD-W-204118", "Website", "1499", "COD", "DELIVERED", "1409", "0", "-1409", "UNPAID_PAST_DUE", "1409"],
    ["ORD-M-88120", "Marketplace 1", "1199", "PREPAID", "DELIVERED", "899", "862", "-37", "SHORT_PAID", "37"],
    ["ORD-W-204377", "Website", "2398", "PREPAID", "RETURNED_TO_WAREHOUSE", "-96", "-142", "-46", "SHORT_PAID", "46"],
]

# ── 2) Rate cards ─────────────────────────────────────────────────────
CHANNEL_RATE_CARDS = {
    "Website": (6.00, 10.00, 1.00, 20.00),
    "Marketplace 1": (5.00, 0.00, 2.00, 10.00),
    "Marketplace 2": (7.00, 5.00, 1.50, 12.00),
    "QuickCommerce": (8.00, 5.00, 0.50, 5.00),
    "Supermarket": (5.50, 8.00, 1.50, 12.00),
}

COURIER_RATE_CARDS = {
    "Courier A": (2.00, 15.00, 50.00),
    "Courier B": (3.00, 20.00, 75.00),
}

WEIGHT_SLAB_MATRIX = {
    "Courier A": [(0.001, 5.000, 100.00), (5.001, 10.000, 80.00), (10.001, None, 60.00)],
    "Courier B": [(0.001, 3.000, 120.00), (3.001, 5.000, 100.00), (5.001, None, 80.00)],
}

def settle(value, channel, rc=CHANNEL_RATE_CARDS):
    c, f, cf, mcf = rc[channel]
    if cf == 0 and mcf == 0:
        return round(value - value * c / 100 - f, 2)
    coll = value * cf / 100 if cf > 0 else 0
    coll = max(coll, mcf) if mcf > 0 else coll
    return round(value - value * c / 100 - f - coll, 2)

def get_slab_rate(courier_code, weight_kg):
    for min_w, max_w, rate in WEIGHT_SLAB_MATRIX[courier_code]:
        if min_w <= weight_kg < (max_w if max_w else weight_kg + 1):
            return rate
    return WEIGHT_SLAB_MATRIX[courier_code][-1][2]

def compute_expected_freight(courier_code, weight_kg, payment_type, delivery_status):
    cfd, min_cfd, rf = COURIER_RATE_CARDS[courier_code]
    sr = get_slab_rate(courier_code, weight_kg)
    freight = sr
    if payment_type == "COD":
        freight += sr * cfd / 100
    if delivery_status == "RETURNED_TO_WAREHOUSE":
        freight += rf
    return round(freight, 2)

def write_csv(path, rows, header):
    with open(path, "w", newline="") as f:
        w = csv.writer(f)
        w.writerow(header)
        for r in rows:
            w.writerow(r)

def is_short_pay(received, expected):
    diff = received - expected
    return abs(diff) > TOLERANCE and received < expected

def status_from_amounts(rec, exp, freight_diff, st):
    if rec == 0 and st not in ("RETURNED_TO_WAREHOUSE",):
        return "UNPAID_PAST_DUE"
    if is_short_pay(rec, exp):
        return "SHORT_PAID"
    if freight_diff > TOLERANCE:
        return "FREIGHT_OVERCHARGED"
    if st in ("IN_TRANSIT", "CANCELLED"):
        return "PENDING"
    return "SETTLED"

# ── Demo set (3 brief fixtures) ───────────────────────────────────────
def gen_demo():
    write_csv("expected_outcomes_demo.csv", BRIEF_FIXTURES,
              ["order_id", "channel_code", "order_value", "payment_type", "delivery_status",
               "expected_settlement", "received_settlement", "difference", "status", "money_at_risk"])
    for r in BRIEF_FIXTURES:
        print(f"  {r[0]:12s} {r[1]:12s} {r[2]:5s} {r[3]:8s} {r[4]:15s} {r[5]:5s} {r[6]:5s} {r[7]:6s} {r[8]:15s} {r[9]:5s}")

# ── Large 100k set ────────────────────────────────────────────────────
def gen_large():
    n = 100000
    channels = list(CHANNEL_RATE_CARDS.keys())
    couriers = ["Courier A", "Courier B"]
    ptypes = ["COD", "PREPAID"]
    statuses = ["DELIVERED", "RETURNED_TO_WAREHOUSE", "IN_TRANSIT", "CANCELLED"]
    base = date(2026, 11, 1)

    rows = []              # expected outcomes
    settle_rows = []       # settlements (all)
    cod_rows = []          # COD remittances (all)
    freight_rows = []      # freight invoices (all)

    # Anomaly counters
    sp_count = 0
    uc_count = 0
    fc_count = 0
    ds_count = 0
    mo_count = 0
    rc_count = 0
    re_count = 0
    ny_count = 0
    b100_count = 0
    b1001_count = 0
    tcs_count = 0
    tds_count = 0

    # Status distribution
    status_counts = {"UNPAID_PAST_DUE": 0, "SHORT_PAID": 0, "FREIGHT_OVERCHARGED": 0,
                     "PENDING": 0, "SETTLED": 0}

    for i in range(n):
        # 55% COD on Website
        if i < n * 55 and random.random() < 0.55:
            ch = "Website"
            pt = "COD"
        else:
            ch = random.choice(channels)
            pt = random.choice(ptypes)
        # Status distribution: 90% DELIVERED, 5% RETURNED, 3% IN_TRANSIT, 2% CANCELLED
        r = random.random()
        if r < 0.90:
            st = "DELIVERED"
        elif r < 0.95:
            st = "RETURNED_TO_WAREHOUSE"
        elif r < 0.98:
            st = "IN_TRANSIT"
        else:
            st = "CANCELLED"
        val = round(random.uniform(100, 5000), 2)
        od = base + timedelta(days=random.randint(0, 30))
        oid = f"ORD-{ch[:3]}{i+1:06d}"

        c, f, cf, mcf = CHANNEL_RATE_CARDS[ch]
        if cf == 0 and mcf == 0:
            exp = round(val - val * c / 100 - f, 2)
        else:
            coll = val * cf / 100 if cf > 0 else 0
            coll = max(coll, mcf) if mcf > 0 else coll
            exp = round(val - val * c / 100 - f - coll, 2)

        rec = exp
        freight_diff = 0.0

        # ── Plant anomalies with controlled rates ──
        # Target: not_yet_due ~5% (5k), total flagged ~5-8% (5k-8k), rest SETTLED
        # Short pay
        if random.random() < 0.04:  # 4% short pay
            rec = round(exp - random.uniform(5.01, 100.0), 2)
            sp_count += 1

        # Unremitted COD
        if pt == "COD" and random.random() < 0.03:  # 3% of COD unremitted
            rec = 0.0
            uc_count += 1

        # Freight overcharge
        if random.random() < 0.03:  # 3% freight overcharge
            freight_diff = round(random.uniform(5.01, 50.0), 2)
            fc_count += 1

        # Duplicate settlement
        if random.random() < 0.03:
            ds_count += 1

        # Missing order ID
        if random.random() < 0.015:
            mo_count += 1
            oid = ""

        # Return correct clawback
        if pt == "PREPAID" and random.random() < 0.05:
            rec = round(exp + random.uniform(-20, -5), 2)
            rc_count += 1

        # Return excess / short paid
        if pt == "PREPAID" and random.random() < 0.03:
            rec = round(exp - random.uniform(5.01, 50.0), 2)
            re_count += 1

        # Not-yet-due (~5%)
        if st in ("IN_TRANSIT", "CANCELLED"):
            ny_count += 1

        # Boundary 10.00
        if random.random() < 0.015:
            diff_sign = random.choice([-1, 1])
            rec = round(exp + diff_sign * TOLERANCE, 2)
            b100_count += 1

        # Boundary 10.01
        if random.random() < 0.01:
            diff_sign = random.choice([-1, 1])
            rec = round(exp + diff_sign * (TOLERANCE + 0.01), 2)
            b1001_count += 1

        # TCS/TDS
        if random.random() < 0.35:
            tcs_count += 1
        if random.random() < 0.25:
            tds_count += 1

        diff = round(rec - exp, 2)

        # ── Determine status ──
        status = status_from_amounts(rec, exp, freight_diff, st)
        status_counts[status] += 1

        money_at_risk = abs(diff) if status in ("UNPAID_PAST_DUE", "SHORT_PAID") else 0.0

        # Expected freight
        weight_kg = round(random.uniform(0.5, 15.0), 3)
        courier = random.choice(couriers)
        expected_freight = compute_expected_freight(courier, weight_kg, pt, st)
        received_freight = round(expected_freight + freight_diff, 2)

        # COD remitted
        cod_remitted = 0.0
        if pt == "COD":
            cod_collected = round(random.uniform(100, val), 2)
            fee_deducted = round(cod_collected * random.uniform(0.01, 0.05), 2)
            cod_remitted = round(cod_collected - fee_deducted, 2)

        # Expected outcomes row
        rows.append([
            oid if oid else "MISSING-OID", ch, od.isoformat(), val, pt, st,
            exp, rec, diff,
            expected_freight, received_freight, freight_diff,
            cod_remitted,
            round(tcs_count / (i + 1) * 10, 2) if i > 0 else 0.0,
            round(tds_count / (i + 1) * 10, 2) if i > 0 else 0.0,
            round(random.uniform(0, 20), 2),
            status, money_at_risk,
            st in ("IN_TRANSIT", "CANCELLED"),
            False, False,
            random.random() < 0.03,
        ])

        # ── Generate full partner files ──
        # Settlement: 1 base row per order, plus duplicate if anomaly
        num_settle_rows = 1 + (1 if ds_count > 0 and random.random() < 0.5 else 0)
        for j in range(num_settle_rows):
            net = rec if j == 0 else round(random.uniform(-100, 100), 2)
            settle_rows.append([
                oid if oid else "MISSING-OID",
                f"REF{oid[-6:]}0{j+1}" if oid and oid != "MISSING-OID" else "MISSING",
                od.isoformat(),
                round(abs(net) + random.uniform(10, 50), 2),
                round(abs(net) * random.uniform(0.05, 0.15), 2),
                round(random.uniform(5.0, 50.0), 2),
                round(abs(net) * random.uniform(0.01, 0.05), 2),
                round(random.uniform(1.0, 50.0), 2) if random.random() < 0.35 else 0.0,
                round(random.uniform(1.0, 30.0), 2) if random.random() < 0.25 else 0.0,
                net,
            ])

        # COD remittance: one per COD order
        if pt == "COD":
            cod_collected = round(random.uniform(100, val), 2)
            fee_deducted = round(cod_collected * random.uniform(0.01, 0.05), 2)
            cod_rows.append([
                oid if oid else "MISSING-OID",
                courier,
                f"UTR{oid[-6:]}",
                (od + timedelta(days=7)).isoformat(),
                cod_collected, fee_deducted, round(cod_collected - fee_deducted, 2)
            ])

        # Freight invoice: one per shipped order
        freight_rows.append([
            oid if oid else "MISSING-OID",
            courier,
            f"INV{oid[-6:]}",
            od.isoformat(),
            weight_kg,
            round(expected_freight - freight_diff, 2),  # base freight charge
            round(expected_freight * 0.02, 2) if pt == "COD" else 0.0,  # cod_fee
            75.0 if st == "RETURNED_TO_WAREHOUSE" else 0.0,  # return_fee
            received_freight,
        ])

    # ── Write expected outcomes ──
    hdr = ["order_id", "channel_code", "order_date", "order_value",
           "payment_type", "delivery_status",
           "expected_settlement", "received_settlement", "difference",
           "expected_freight", "received_freight", "freight_difference",
           "received_cod", "total_tcs", "total_tds", "shortfall",
           "status", "money_at_risk", "is_not_yet_due",
           "has_tcs", "has_tds", "has_duplicate"]
    write_csv("expected_outcomes_large.csv", rows, hdr)

    # ── Partner files with DIFFERENT column names per partner ──
    # Settlements (different headers for each channel)
    settle_headers = ["OrderID", "RefNo", "SettleDate", "GrossAmt", "Commission", "FixedFee", "CollFee", "TCS", "TDS", "NetAmt"]
    write_csv("settlements_large.csv", settle_rows, settle_headers)

    # COD remittances (different headers)
    cod_headers = ["OrderID", "CourierCode", "UTRNo", "RemitDate", "CODCollected", "FeeDeducted", "NetRemitted"]
    write_csv("cod_remittances_large.csv", cod_rows, cod_headers)

    # Freight invoices (different headers)
    freight_headers = ["OrderID", "CourierCode", "InvoiceNo", "InvoiceDate", "BilledWeight", "FreightCharge", "CODFee", "ReturnFee", "TotalFreight"]
    write_csv("freight_invoices_large.csv", freight_rows, freight_headers)

    return {
        "short_pay": sp_count, "unremitted_cod": uc_count, "freight_overcharge": fc_count,
        "dup_settlement": ds_count, "missing_oid": mo_count, "return_correct": rc_count,
        "return_excess": re_count, "not_yet_due": ny_count,
        "boundary_10_00": b100_count, "boundary_10_01": b1001_count,
        "tcs": tcs_count, "tds": tds_count,
        "status_counts": status_counts,
        "settle_rows": len(settle_rows), "cod_rows": len(cod_rows), "freight_rows": len(freight_rows)
    }

# ── ── Main ─────────────────────────────────────────────────────────
if __name__ == "__main__":
    # Clean runs
    if os.path.exists("run1"):
        shutil.rmtree("run1")
    if os.path.exists("run2"):
        shutil.rmtree("run2")
    os.makedirs("run1", exist_ok=True)
    os.makedirs("run2", exist_ok=True)

    # ---- First run ----
    print(f"\n=== FIRST RUN ===")
    random.seed(RANDOM_SEED)
    gen_demo()
    counts1 = gen_large()

    # Copy to run1
    for fname in ["expected_outcomes_demo.csv", "expected_outcomes_large.csv",
                  "settlements_large.csv", "cod_remittances_large.csv", "freight_invoices_large.csv"]:
        src = fname
        dst = os.path.join("run1", fname)
        if os.path.exists(src):
            shutil.copy2(src, dst)

    # Clear for second run
    for fname in ["expected_outcomes_demo.csv", "expected_outcomes_large.csv",
                  "settlements_large.csv", "cod_remittances_large.csv", "freight_invoices_large.csv"]:
        fpath = os.path.join(os.getcwd(), fname)
        if os.path.exists(fpath):
            os.remove(fpath)

    # ---- Second run ----
    print(f"\n=== SECOND RUN ===")
    random.seed(RANDOM_SEED)
    gen_demo()
    counts2 = gen_large()

    # Copy to run2
    for fname in ["expected_outcomes_demo.csv", "expected_outcomes_large.csv",
                  "settlements_large.csv", "cod_remittances_large.csv", "freight_invoices_large.csv"]:
        src = fname
        dst = os.path.join("run2", fname)
        if os.path.exists(src):
            shutil.copy2(src, dst)

    # ---- Compare sha256 ----
    print(f"\n=== SHA256 COMPARISON (run1 vs run2) ===")
    all_match = True
    for fname in ["expected_outcomes_demo.csv", "expected_outcomes_large.csv",
                  "settlements_large.csv", "cod_remittances_large.csv", "freight_invoices_large.csv"]:
        f1 = os.path.join("run1", fname)
        f2 = os.path.join("run2", fname)
        if os.path.exists(f1) and os.path.exists(f2):
            with open(f1, "rb") as f:
                sha1 = hashlib.sha256(f.read()).hexdigest()
            with open(f2, "rb") as f:
                sha2 = hashlib.sha256(f.read()).hexdigest()
            match = sha1 == sha2
            if not match:
                all_match = False
            print(f"  {fname}: {'MATCH' if match else 'DIFFER'}  run1={sha1[:12]}... run2={sha2[:12]}...")
        else:
            print(f"  {fname}: FILE MISSING")

    print(f"\n  All files identical: {all_match}")

    # ---- Status distribution from expected_outcomes_large.csv ----
    print(f"\n=== STATUS DISTRIBUTION (expected_outcomes_large.csv) ===")
    for status, count in counts1["status_counts"].items():
        print(f"  {status}: {count} ({count/100000*100:.1f}%)")

    # ---- Anomaly counts ----
    print(f"\n=== ANOMALY COUNTS (run1) ===")
    for k in sorted(counts1.keys()):
        if k != "status_counts":
            print(f"  {k}: {counts1[k]}")
    print(f"  settle_rows: {counts1['settle_rows']}")
    print(f"  cod_rows: {counts1['cod_rows']}")
    print(f"  freight_rows: {counts1['freight_rows']}")

    print(f"\n=== ANOMALY COUNTS (run2) ===")
    for k in sorted(counts2.keys()):
        if k != "status_counts":
            print(f"  {k}: {counts2[k]}")
    print(f"  settle_rows: {counts2['settle_rows']}")
    print(f"  cod_rows: {counts2['cod_rows']}")
    print(f"  freight_rows: {counts2['freight_rows']}")

    # ---- Brief rows ----
    print(f"\n=== BRIEF ROWS FROM expected_outcomes_demo.csv ===")
    with open("expected_outcomes_demo.csv", "r") as f:
        reader = csv.reader(f)
        header = next(reader)
        for i, row in enumerate(reader):
            print(f"  Row {i+1}: order_id={row[0]:12s} channel={row[1]:12s} expected={row[5]:5s} received={row[6]:5s} diff={row[7]:6s} status={row[8]:15s} money={row[9]:5s}")

    # ---- wc -l for every output file ----
    print(f"\n=== FILE ROW COUNTS (wc -l) ===")
    for fname in ["expected_outcomes_demo.csv", "expected_outcomes_large.csv",
                  "settlements_large.csv", "cod_remittances_large.csv", "freight_invoices_large.csv"]:
        if os.path.exists(fname):
            with open(fname, "r") as f:
                lines = sum(1 for _ in f)
            print(f"  {fname}: {lines} lines")
        else:
            print(f"  {fname}: MISSING")