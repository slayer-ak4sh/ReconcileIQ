-- Flyway Migration V3: Seed rate cards and default data
-- This migration seeds channel and courier rate cards used by the demo dataset

-- Channel rate cards (2 channels from the brief: Website, Marketplace 1)
INSERT INTO channel_rate_cards (channel_code, commission_pct, fixed_fee, collection_fee_pct, min_collection_fee, effective_from) VALUES
('Website', 6.00, 10.00, 1.00, 20.00, CURRENT_DATE),
('Marketplace 1', 5.00, 0.00, 2.00, 10.00, CURRENT_DATE);

-- Courier rate cards (2 couriers from the brief)
INSERT INTO courier_rate_cards (courier_code, cod_fee_pct, min_cod_fee, return_fee, effective_from) VALUES
('Courier A', 2.00, 15.00, 50.00, CURRENT_DATE),
('Courier B', 3.00, 20.00, 75.00, CURRENT_DATE);

-- Courier weight slabs (Website typically uses Courier A, Marketplace uses Courier B)
INSERT INTO courier_weight_slabs (rate_card_id, min_weight_kg, max_weight_kg, rate) VALUES
-- Courier A slabs (Website)
(1, 0.001, 5.000, 100.00),
(1, 5.001, 10.000, 80.00),
(1, 10.001, NULL, 60.00),
-- Courier B slabs (Marketplace 1)
(2, 0.001, 3.000, 120.00),
(2, 3.001, 5.000, 100.00),
(2, 5.001, NULL, 80.00);

-- Demo sources
INSERT INTO sources (name, code, partner_type, channel_code, courier_code, created_at) VALUES
('Website', 'WEBSITE', 'CHANNEL', 'Website', 'Courier A', CURRENT_DATE),
('Marketplace 1', 'MP1', 'CHANNEL', 'Marketplace 1', 'Courier B', CURRENT_DATE);

-- Demo settings
INSERT INTO settings (setting_key, setting_value, description, updated_at) VALUES
('tolerance_default', '10.00', 'Default tolerance in INR', CURRENT_DATE),
('tcs_pct', '1.00', 'Tax Collected at Source percentage', CURRENT_DATE),
('tds_pct', '2.00', 'Tax Deducted at Source percentage', CURRENT_DATE);