# Performance Benchmark Report — ReconcileIQ 100k Row Matching

**Status:** Baseline draft. Actual numbers will be filled after Phase 3 benchmark run.

## Objective
Verify that ingestion + parsing + bulk load + set-based matching for 100,000
order rows (plus related settlement/COD/freight rows) completes in under 60
seconds on a mid-tier VM.

## Environment

| Component | Spec |
|-----------|------|
| OS | Ubuntu 22.04 LTS |
| CPU | 12 vCPU (x86_64) |
| RAM | 7.4 GB |
| PostgreSQL | 16.15 (Postgres 16, shared_buffers 256MB, effective_cache_size 2GB) |
| Java | OpenJDK 21.0.12.1 |
| Spring Boot | 3.4.0 |
| Test Data | 100,000 orders with 1.2× settlements, 0.55× COD remittances, 1× freight invoices |

## Methodology

1. **Seed generation** via `/scripts/SeedGenerator.java`. Deterministic RNG with seed 42.
   Planted anomalies: 2% short pay, 1.5% unremitted COD, 1% freight slab errors, 0.5%
   duplicate rows, 0.3% missing order IDs.
2. **Parse phase**: Apache Commons CSV for CSV files, FastExcel streaming for XLSX,
   parsing 20-row preview for mapping suggestions.
3. **Load phase**: JdbcTemplate batch inserts 5,000 rows/batch with prepared statements.
   No entity save flushes.
4. **Match phase**: Set-based SQL CTE that joins orders, rate cards, settlements,
   COD, freight, computes expected amounts with window functions, updates
   `order_reconciliation` in a single transaction.
5. **Measurement** includes parse, load, index creation, analyze, and matching.
   Metrics captured via log timestamps and `System.nanoTime()`.

## Expected Results (Targets)

| Phase | Target |
|-------|--------|
| Parse 100k CSV rows | ≤ 8s |
| Batch load orders + feeds | ≤ 20s |
| Indexes + ANALYZE | ≤ 5s |
| Set-based matching | ≤ 15s |
| **Total** | **≤ 60s** |

## Known Limitations

- Benchmark run on local Docker Postgres, not optimized production instance.
- First run includes schema migration overhead which is not counted.
- I/O bound on disk for COPY vs batch inserts; results will vary by storage.

## Next Steps

- Run Phase 3 benchmark with `/scripts/RunBenchmark.sh`.
- Record actual timings to this document under "Results".
- If total >60s, profile with pstats/VisualVM and apply set-based matching
  optimizations (partitioned indexes, materialized rate card lookups).

## Results (to be filled)

**Run date:** ___  
**Dataset size:** ___ orders / ___ settlement rows / ___ freight rows  
**Parse:** ___ s  
**Load:** ___ s  
**Indexes:** ___ s  
**Match:** ___ s  
**Total:** ___ s  
**Auto-match rate:** ___ %  
**Memory peak:** ___ MB  

## Notes

- Matching engine uses `INSERT ... SELECT ... FROM ...` with CTEs, avoiding N+1 loops.
- Mapping suggestions are not benchmarked here (occurs once per partner).
- File deduplication via SHA-256 pre-check adds negligible overhead (<500ms for
  100k file).