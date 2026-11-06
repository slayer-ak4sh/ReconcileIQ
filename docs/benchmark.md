# Benchmark: ReconcileIQ 100k-Row Processing Performance

## Target
Under 60 seconds for: parse 100k rows → load into DB → run matching → produce order_reconciliation.

## Environment
- Docker Compose: backend (Spring Boot 21 + JdbcTemplate), postgres (16)
- 100k-row CSV: order_id, channel_code, order_date, order_value, payment_type, delivery_status
- Rate cards seeded via Flyway V3 (Website: 6%/10/1%, Marketplace 1: 5%/0/2%)
- Indexes: order_id, channel_code, order_date, reconciliation_status, due_date

## Status: NOT YET RUN
Benchmark has not yet been executed on this codebase. Target: ingest + match under 60 s.

## Planned Method
1. Load the 100k set through the REAL ingestion path (CSV upload with saved mappings), not a shortcut.
2. Use streaming parse and COPY or large JDBC batches.
3. Measure separately: parse, load (DB write), match, total.
4. Run 3 times on a fresh database, report each run and the median.
5. Report total rows per file and combined, machine (CPU, RAM, disk, OS), Java and PostgreSQL versions, key Postgres settings, heap size, peak memory if measurable, and limitations (local laptop, Docker, single user).
6. If over 60 s, profile with EXPLAIN (ANALYZE, BUFFERS) on the match query, optimise (COPY, bigger batches, indexes before match, ANALYZE, temp tables, unlogged staging), and show before and after numbers.

If it still misses the target, say so plainly with the numbers.