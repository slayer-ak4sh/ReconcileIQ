-- Flyway Migration V1: Schema Initialization for ReconcileIQ

-- 1. Users
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Sources (Sales channels & Couriers)
CREATE TABLE sources (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    partner_type VARCHAR(20) NOT NULL, -- CHANNEL or COURIER
    channel_code VARCHAR(50),
    courier_code VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. Column Mappings
CREATE TABLE column_mappings (
    id BIGSERIAL PRIMARY KEY,
    source_id BIGINT NOT NULL REFERENCES sources(id) ON DELETE CASCADE,
    file_type VARCHAR(50) NOT NULL, -- ORDERS, SETTLEMENT, COD_REMITTANCE, FREIGHT_INVOICE, WAREHOUSE_SCAN
    version INT NOT NULL,
    mappings_json TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(255) NOT NULL,
    approved_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_source_file_version UNIQUE(source_id, file_type, version)
);

-- 4. Upload Batches
CREATE TABLE upload_batches (
    id BIGSERIAL PRIMARY KEY,
    source_id BIGINT NOT NULL REFERENCES sources(id),
    file_type VARCHAR(50) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_hash VARCHAR(64) NOT NULL,
    row_count INT NOT NULL DEFAULT 0,
    valid_row_count INT NOT NULL DEFAULT 0,
    error_count INT NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL, -- PROCESSING, COMPLETED, FAILED
    uploaded_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 5. Orders (Master Feed)
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    order_id VARCHAR(100) NOT NULL UNIQUE,
    channel_code VARCHAR(50) NOT NULL,
    order_date DATE NOT NULL,
    order_value NUMERIC(12, 2) NOT NULL,
    payment_type VARCHAR(20) NOT NULL, -- COD or PREPAID
    delivery_status VARCHAR(50) NOT NULL, -- DELIVERED, RETURNED_TO_WAREHOUSE, IN_TRANSIT, CANCELLED
    customer_name VARCHAR(255),
    customer_phone VARCHAR(50),
    customer_address TEXT,
    batch_id BIGINT REFERENCES upload_batches(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 6. Settlements
CREATE TABLE settlements (
    id BIGSERIAL PRIMARY KEY,
    batch_id BIGINT NOT NULL REFERENCES upload_batches(id),
    order_id VARCHAR(100) NOT NULL,
    channel_code VARCHAR(50) NOT NULL,
    reference_no VARCHAR(100),
    settlement_date DATE NOT NULL,
    gross_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    commission NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    fixed_fee NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    collection_fee NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    tcs_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    tds_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    net_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 7. COD Remittances
CREATE TABLE cod_remittances (
    id BIGSERIAL PRIMARY KEY,
    batch_id BIGINT NOT NULL REFERENCES upload_batches(id),
    order_id VARCHAR(100) NOT NULL,
    courier_code VARCHAR(50) NOT NULL,
    utr_no VARCHAR(100),
    remittance_date DATE NOT NULL,
    cod_collected NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    fee_deducted NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    net_remitted NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 8. Freight Invoices
CREATE TABLE freight_invoices (
    id BIGSERIAL PRIMARY KEY,
    batch_id BIGINT NOT NULL REFERENCES upload_batches(id),
    order_id VARCHAR(100) NOT NULL,
    courier_code VARCHAR(50) NOT NULL,
    invoice_no VARCHAR(100),
    invoice_date DATE NOT NULL,
    billed_weight NUMERIC(8, 3) NOT NULL DEFAULT 0.000,
    freight_charge NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    cod_fee NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    return_fee NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    total_freight NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 9. Returns Scans (P1 Depth / Warehouse Scans)
CREATE TABLE returns_scans (
    id BIGSERIAL PRIMARY KEY,
    batch_id BIGINT NOT NULL REFERENCES upload_batches(id),
    order_id VARCHAR(100) NOT NULL,
    courier_code VARCHAR(50),
    waybill_no VARCHAR(100),
    scan_date DATE NOT NULL,
    warehouse_location VARCHAR(100),
    condition_status VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 10. Channel Rate Cards
CREATE TABLE channel_rate_cards (
    id BIGSERIAL PRIMARY KEY,
    channel_code VARCHAR(50) NOT NULL UNIQUE,
    commission_pct NUMERIC(5, 2) NOT NULL DEFAULT 0.00,
    fixed_fee NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    collection_fee_pct NUMERIC(5, 2) NOT NULL DEFAULT 0.00,
    min_collection_fee NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    effective_from DATE NOT NULL DEFAULT CURRENT_DATE
);

-- 11. Courier Rate Cards
CREATE TABLE courier_rate_cards (
    id BIGSERIAL PRIMARY KEY,
    courier_code VARCHAR(50) NOT NULL UNIQUE,
    cod_fee_pct NUMERIC(5, 2) NOT NULL DEFAULT 0.00,
    min_cod_fee NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    return_fee NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    effective_from DATE NOT NULL DEFAULT CURRENT_DATE
);

-- 12. Courier Weight Slabs
CREATE TABLE courier_weight_slabs (
    id BIGSERIAL PRIMARY KEY,
    rate_card_id BIGINT NOT NULL REFERENCES courier_rate_cards(id) ON DELETE CASCADE,
    min_weight_kg NUMERIC(8, 3) NOT NULL,
    max_weight_kg NUMERIC(8, 3) NOT NULL,
    rate NUMERIC(12, 2) NOT NULL
);

-- 13. Order Reconciliation Details
CREATE TABLE order_reconciliation (
    id BIGSERIAL PRIMARY KEY,
    order_id VARCHAR(100) NOT NULL UNIQUE REFERENCES orders(order_id) ON DELETE CASCADE,
    due_date DATE NOT NULL,
    expected_settlement NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    expected_freight NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    received_settlement NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    received_freight NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    settlement_diff NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    freight_diff NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    total_tcs NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    total_tds NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    reconciliation_status VARCHAR(50) NOT NULL, -- SETTLED, SHORT_PAID, UNPAID_PAST_DUE, FREIGHT_OVERCHARGED, RETURN_NOT_RECEIVED
    is_matched BOOLEAN NOT NULL DEFAULT FALSE,
    matched_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 14. Review Queue
CREATE TABLE review_queue (
    id BIGSERIAL PRIMARY KEY,
    batch_id BIGINT NOT NULL REFERENCES upload_batches(id),
    file_type VARCHAR(50) NOT NULL,
    source_id BIGINT NOT NULL REFERENCES sources(id),
    raw_data_json TEXT NOT NULL,
    suggested_order_id VARCHAR(100),
    confidence NUMERIC(5, 2),
    reason TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, RESOLVED, IGNORED
    reviewed_by VARCHAR(255),
    reviewed_at TIMESTAMP WITH TIME ZONE
);

-- 15. Claims
CREATE TABLE claims (
    id BIGSERIAL PRIMARY KEY,
    claim_number VARCHAR(100) NOT NULL UNIQUE,
    partner_code VARCHAR(50) NOT NULL,
    partner_type VARCHAR(20) NOT NULL, -- CHANNEL or COURIER
    total_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT', -- DRAFT, SUBMITTED, APPROVED, REJECTED, RECOVERED
    created_by VARCHAR(255) NOT NULL,
    submitted_at TIMESTAMP WITH TIME ZONE,
    decided_by VARCHAR(255),
    decided_at TIMESTAMP WITH TIME ZONE,
    decision_notes TEXT,
    recovered_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 16. Claim Items
CREATE TABLE claim_items (
    id BIGSERIAL PRIMARY KEY,
    claim_id BIGINT NOT NULL REFERENCES claims(id) ON DELETE CASCADE,
    order_id VARCHAR(100) NOT NULL REFERENCES orders(order_id),
    order_reconciliation_id BIGINT REFERENCES order_reconciliation(id),
    claim_type VARCHAR(50) NOT NULL, -- SHORT_PAYMENT, UNREMITTED_COD, FREIGHT_OVERCHARGE, LOST_RETURN
    amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    evidence_notes TEXT
);

-- 17. Claim Event Log
CREATE TABLE claim_events (
    id BIGSERIAL PRIMARY KEY,
    claim_id BIGINT NOT NULL REFERENCES claims(id) ON DELETE CASCADE,
    from_status VARCHAR(50) NOT NULL,
    to_status VARCHAR(50) NOT NULL,
    actor_email VARCHAR(255) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 18. Audit Log
CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    actor_email VARCHAR(255) NOT NULL,
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    resource_id VARCHAR(100),
    details_json TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 19. System Settings
CREATE TABLE settings (
    id BIGSERIAL PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_orders_order_id ON orders(order_id);
CREATE INDEX idx_orders_channel ON orders(channel_code);
CREATE INDEX idx_orders_date ON orders(order_date);

CREATE INDEX idx_settlements_order ON settlements(order_id);
CREATE INDEX idx_cod_order ON cod_remittances(order_id);
CREATE INDEX idx_freight_order ON freight_invoices(order_id);
CREATE INDEX idx_returns_order ON returns_scans(order_id);

CREATE INDEX idx_recon_status ON order_reconciliation(reconciliation_status);
CREATE INDEX idx_recon_due_date ON order_reconciliation(due_date);

CREATE INDEX idx_upload_batches_hash ON upload_batches(file_hash);
