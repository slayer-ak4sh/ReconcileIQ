# ARCHITECTURE — ReconcileIQ

## 1. Technical Executive Summary
ReconcileIQ is a modern modular monolith for payment, freight, and returns reconciliation targeting D2C brands. It ingests multi-format financial settlement, COD remittance, freight invoice, and order data from e-commerce platforms and courier aggregators. It standardizes headers via versioned mapping profiles with AI fallback, computes expected values from database-backed rate cards, matches high-volume transaction feeds set-wise using PostgreSQL CTEs, flags payment shortfalls and unremitted COD, and drives partner claims workflows.

## 2. Architecture & Modular Structure
Package by Feature in Java 21 / Spring Boot 3:

- `com.reconcileiq.auth`: User authentication, JWT issuance/validation, RBAC (`FINANCE_EXECUTIVE`, `COFOUNDER`).
- `com.reconcileiq.ingestion`: File upload processing, SHA-256 deduplication, streaming CSV/XLSX parsing, rejected row handling.
- `com.reconcileiq.mapping`: Canonical field definitions, headers-to-canonical mapping versions, AI mapping suggestions (Rule-based Jaro-Winkler + optional LLM fallback).
- `com.reconcileiq.ratecard`: Channel rate cards (commission, fixed fee, collection fee) & courier rate cards (weight slabs, COD fee, return fee).
- `com.reconcileiq.matching`: Order-level rules matching engine using PostgreSQL CTE batch set-matching, review queue population for missing IDs.
- `com.reconcileiq.claims`: Claims lifecycle (DRAFT -> SUBMITTED -> APPROVED / REJECTED -> RECOVERED), evidence package generation, CSV export.
- `com.reconcileiq.dashboard`: Aggregated metrics, Money-at-Risk age bucketing (0-7, 8-15, 16-30, 31+ days), partner leakage breakdowns.
- `com.reconcileiq.common`: Exception handling (`@ControllerAdvice`), security utilities, PII masking utilities, audit logging.

## 3. Database Schema Overview & Entity Relationships
Tables managed by Flyway (`db/migration/V1__init_schema.sql`):
1. `users` (id, email, password_hash, role, full_name, created_at)
2. `sources` (id, name, code, partner_type, channel_code, courier_code, created_at)
3. `column_mappings` (id, source_id, file_type, version, mappings_json, is_active, created_by, approved_at)
4. `upload_batches` (id, source_id, file_type, file_name, file_hash, row_count, valid_row_count, error_count, status, uploaded_by, created_at)
5. `orders` (id, order_id, channel_code, order_date, order_value, payment_type, delivery_status, customer_name, customer_phone, customer_address, created_at)
6. `settlements` (id, batch_id, order_id, channel_code, reference_no, settlement_date, gross_amount, commission, fixed_fee, collection_fee, tcs_amount, tds_amount, net_amount, created_at)
7. `cod_remittances` (id, batch_id, order_id, courier_code, UTR_no, remittance_date, cod_collected, fee_deducted, net_remitted, created_at)
8. `freight_invoices` (id, batch_id, order_id, courier_code, invoice_no, invoice_date, billed_weight, freight_charge, cod_fee, return_fee, total_freight, created_at)
9. `returns_scans` (id, batch_id, order_id, courier_code, waybill_no, scan_date, warehouse_location, condition_status, created_at)
10. `channel_rate_cards` (id, channel_code, commission_pct, fixed_fee, collection_fee_pct, min_collection_fee, effective_from)
11. `courier_rate_cards` (id, courier_code, cod_fee_pct, min_cod_fee, return_fee, effective_from)
12. `courier_weight_slabs` (id, rate_card_id, min_weight_kg, max_weight_kg, rate)
13. `order_reconciliation` (id, order_id, due_date, expected_settlement, expected_freight, received_settlement, received_freight, settlement_diff, freight_diff, total_tcs, total_tds, reconciliation_status, is_matched, matched_at)
14. `review_queue` (id, batch_id, file_type, source_id, raw_data_json, suggested_order_id, confidence, reason, status, reviewed_by, reviewed_at)
15. `claims` (id, claim_number, partner_code, partner_type, total_amount, status, created_by, submitted_at, decided_by, decided_at, decision_notes, recovered_at)
16. `claim_items` (id, claim_id, order_id, order_reconciliation_id, claim_type, amount, evidence_notes)
17. `claim_events` (id, claim_id, from_status, to_status, actor_email, notes, created_at)
18. `audit_log` (id, actor_email, action, resource_type, resource_id, details_json, created_at)
19. `settings` (id, setting_key, setting_value, description, updated_at)

## 4. Performance & High-Volume Strategy (100k rows < 60s)
1. **Streaming Parsing**: Apache Commons CSV & FastExcel SAX parser process rows standard chunk-by-chunk without loading full workbooks into memory.
2. **Bulk Ingestion**: `JdbcTemplate.batchUpdate` (or PostgreSQL `COPY`) inserts in batches of 5,000 rows.
3. **Set-Based Matching**: Matching is executed via a set-based SQL query using Common Table Expressions (CTEs) that join orders, rate cards, settlements, COD remittances, and freight invoices in database memory. Avoids N+1 Java entity loads.
4. **Targeted Indexes**: B-tree indexes on `order_id`, `(channel_code, order_date)`, `(courier_code, invoice_date)`, `reconciliation_status`, and foreign keys.

## 5. Security & Privacy
1. **DPDP Compliance**: Masking utility `PiiMasker` obfuscates customer names, phones, and addresses in DTO mappings prior to API response serialization.
2. **RBAC**: Spring Security annotations `@PreAuthorize("hasRole('FINANCE_EXECUTIVE')")` and `@PreAuthorize("hasRole('COFOUNDER')")` enforce authorization at the service/controller boundaries.
3. **JWT Stateless Authentication**: Short-lived JWTs signed with HMAC-SHA256, carrying user email and role.
4. **Input & Upload Security**: File extension validation (`.csv`, `.xlsx`), MIME type verification, SHA-256 file hash checks for duplicate prevention, size limits (max 50MB). Parametrized JPA and SQL to eliminate SQL injection.
