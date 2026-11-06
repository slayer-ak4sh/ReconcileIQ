#!/usr/bin/env python3
"""
Deterministic Seed Generator for ReconcileIQ Phase 3

Produces deterministic output with fixed random seed.
Run twice → identical sha256sum for all output files.
"""

import csv
import hashlib
import random
from datetime import date

RANDOM_SEED = 42
random.seed(RANDOM_SEED)
TOLERANCE = 10.00

# ── Channel rate cards ──────────────────────────────────────────────
CHANNEL_RATE_CARDS = {
    "Website": (6.00, 10.00, 1.00, 20.00),
    "Marketplace 1": (5.00, 0.00, 2.00, 10.00),
}

def settle(value, channel):
    c, f, cf, mcf = CHANNEL_RATE_CARDS[channel]
    return round(value - value * c / 100 - f - max(value * cf / 100, mcf), 2)

# ── Write CSV ──────────────────────────────────────────────────────
def write_csv(path, rows, header):
    with open(path, "w", newline="") as f:
        w = csv.writer(f)
        w.writerow(header)
        for r in rows:
            w.writerow(r)

# ── ── Demo set ────────────────────────────────────────────────────
def gen_demo():
    rows = [
        ["ORD-W-204118", "Website", "1499", "COD", "DELIVERED", "1379.06", "0.00", "-1379.06", "UNPAID_PAST_DUE", "1379.06"],
        ["ORD-M-88120", "Marketplace 1", "1199", "PREPAID", "DELIVERED", "1115.07", "862.00", "-253.07", "SHORT_PAID", "253.07"],
        ["ORD-W-204377", "Website", "2398", "PREPAID", "RETURNED_TO_WAREHOUSE", "-2220.14", "-142.00", "2078.14", "FREIGHT_OVERCHARGED", "0.00"],
    ]
    write_csv("expected_outcomes_demo.csv", rows,
              ["order_id", "channel_code", "order_value", "payment_type", "delivery_status",
               "expected_settlement", "received_settlement", "difference", "status", "money_at_risk"])
    return "demo"

# ── ── Large set ───────────────────────────────────────────────────
def gen_large():
    n = 100000
    channels = list(CHANNEL_RATE_CARDS.keys())
    couriers = ["Courier A", "Courier B"]
    ptypes = ["COD", "PREPAID"]
    statuses = ["DELIVERED", "RETURNED_TO_WAREHOUSE", "IN_TRANSIT", "CANCELLED"]

    rows = []
    anomalies = {"short_pay": 0, "unremitted_cod": 0, "freight_overcharge": 0,
                 "dup_settle": 0, "not_yet_due": 0, "tcs": 0, "tds": 0}

    base = date(2026, 11, 1)
    for i in range(n):
        ch = random.choice(channels)
        cou = random.choice(couriers)
        pt = random.choice(ptypes)
        st = random.choice(statuses)
        val = round(random.uniform(100, 5000), 2)
        od = base + timedelta(days=random.randint(0, 30))
        oid = f"ORD-{ch[:3]}{i+1:06d}"

        exp = settle(val, ch)
        wkg = round(random.uniform(0.5, 15.0), 3)
        exp_frt = round(random.uniform(50, 200), 2)  # simplified

        rec = exp
        anomaly = False

        # short pay >10
        if random.random() < 0.15:
            rec = round(exp - random.uniform(5.01, 100.0), 2)
            anomalies["short_pay"] += 1
            anomaly = True

        # unremitted COD
        if pt == "COD" and random.random() < 0.25:
            rec = 0.0
            anomalies["unremitted_cod"] += 1
            anomaly = True

        # freight overcharge
        if random.random() < 0.10:
            exp_frt += random.uniform(5.01, 50.0)
            anomalies["freight_overcharge"] += 1

        # not yet due
        if st in ("IN_TRANSIT", "CANCELLED"):
            anomalies["not_yet_due"] += 1

        # TCS / TDS
        if random.random() < 0.4:
            anomalies["tcs"] += 1
        if random.random() < 0.3:
            anomalies["tds"] += 1

        rows.append([
            oid, ch, od.isoformat(), val, pt, st,
            exp, rec, round(rec - exp, 2),
            exp_frt, round(random.uniform(50, 200), 2),  # freight diff
            round(random.uniform(0, 20), 2),  # cod remitted
            0.0, 0.0,  # tcs, tds
            0,  # shortfall
            "SETTLED" if not anomaly else ("SHORT_PAID" if rec < exp and abs(rec-exp) > TOLERANCE
                                           else "FREIGHT_OVERCHARGED" if random.random()<0.1 else "SETTLED"),
            abs(rec - exp) if rec < exp and abs(rec-exp) > TOLERANCE else 0.0,
            random.random() < 0.05,  # duplicate flag
        ])

    # Write expected outcomes
    hdr = ["order_id","channel_code","order_date","order_value","payment_type","delivery_status",
           "expected_settlement","received_settlement","difference",
           "expected_freight","received_freight","freight_difference",
           "received_cod","total_tcs","total_tds","shortfall","status","money_at_risk",
           "is_duplicate"]
    write_csv("expected_outcomes_large.csv", rows, hdr)

    with open("expected_outcomes_large.csv", "rb") as f:
        large_sha = hashlib.sha256(f.read()).hexdigest()

    # Write settlements (simple)
    settle_rows = []
    for i in range(min(n, 50)):
        r = rows[i]
        settle_rows.append([r[0], f"REF{r[0][-6:]}01", "2026-11-01",
                           round(abs(r[7])+20, 2), round(abs(r[7])*0.1, 2),
                           5.0, round(abs(r[7])*0.01, 2),
                           round(anomalies["tcs"]/n*10, 2),
                           round(anomalies["tds"]/n*10, 2),
                           r[7]])

    write_csv("settlements_large.csv", settle_rows,
              ["order_id","reference_no","settlement_date","gross_amount",
               "commission","fixed_fee","collection_fee","tcs_amount","tds_amount","net_amount"])

    # Write COD & freight (small samples)
    cod_rows = [["ORD-001", "Courier A", "UTR001", "2026-11-08", 500.00, 25.00, 475.00]]
    write_csv("cod_remittances_large.csv", cod_rows,
              ["order_id","courier_code","utr_no","remittance_date","cod_collected","fee_deducted","net_remitted"])

    fr_rows = [["ORD-001", "Courier A", "INV001", "2026-11-01", 2.5, 100.00, 10.0, 15.0, 115.0]]
    write_csv("freight_invoices_large.csv", fr_rows,
              ["order_id","courier_code","invoice_no","invoice_date",
               "billed_weight","freight_charge","cod_fee","return_fee","total_freight"])

    return large_sha

# ── ── Main ─────────────────────────────────────────────────────────
if __name__ == "__main__":
    print(f"Seed generator (seed={RANDOM_SEED})")
    d = gen_demo()
    print(f"  Demo set written (3 rows)")
    lsha = gen_large()
    print(f"  Large set written (100k rows)")
    print(f"\n  expected_outcomes_demo.csv sha256: {hashlib.sha256(open('expected_outcomes_demo.csv','rb').read()).hexdigest()}")
    print(f"  expected_outcomes_large.csv sha256: {lsha}")
    print("\nRun twice → identical checksums.")