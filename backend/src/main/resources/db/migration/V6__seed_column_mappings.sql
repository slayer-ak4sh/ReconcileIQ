-- Flyway Migration V6: Seed column mappings for demo sources
-- Maps the canonical field names to the partner file headers

-- Website Settlement mapping (source_id=1)
-- Note: channelCode is derived from orders during matching, not from settlement file
INSERT INTO column_mappings (source_id, file_type, version, mappings_json, is_active, created_by, approved_at) VALUES
(1, 'settlement', 1,
 '{"orderId": "OrderID", "referenceNo": "ReferenceNo", "settlementDate": "SettleDate", "grossAmount": "GrossAmt", "commission": "Commission", "fixedFee": "FixedFee", "collectionFee": "CollFee", "tcsAmount": "TCS", "tdsAmount": "TDS", "netAmount": "NetAmt"}',
 true, 'system', CURRENT_DATE);

-- Marketplace 1 Settlement mapping (source_id=2)
INSERT INTO column_mappings (source_id, file_type, version, mappings_json, is_active, created_by, approved_at) VALUES
(2, 'settlement', 1,
 '{"orderId": "OrderID", "referenceNo": "ReferenceNo", "settlementDate": "SettleDate", "grossAmount": "GrossAmt", "commission": "Commission", "fixedFee": "FixedFee", "collectionFee": "CollFee", "tcsAmount": "TCS", "tdsAmount": "TDS", "netAmount": "NetAmt"}',
 true, 'system', CURRENT_DATE);

-- Website COD Remittance mapping (source_id=1)
INSERT INTO column_mappings (source_id, file_type, version, mappings_json, is_active, created_by, approved_at) VALUES
(1, 'cod_remittance', 1,
 '{"orderId": "OrderID", "courierCode": "CourierCode", "utrNo": "UTRNo", "remittanceDate": "RemitDate", "codCollected": "CODCollected", "feeDeducted": "FeeDeducted", "netRemitted": "NetRemitted"}',
 true, 'system', CURRENT_DATE);

-- Marketplace 1 COD Remittance mapping (source_id=2)
INSERT INTO column_mappings (source_id, file_type, version, mappings_json, is_active, created_by, approved_at) VALUES
(2, 'cod_remittance', 1,
 '{"orderId": "OrderID", "courierCode": "CourierCode", "utrNo": "UTRNo", "remittanceDate": "RemitDate", "codCollected": "CODCollected", "feeDeducted": "FeeDeducted", "netRemitted": "NetRemitted"}',
 true, 'system', CURRENT_DATE);

-- Website Freight Invoice mapping (source_id=1)
INSERT INTO column_mappings (source_id, file_type, version, mappings_json, is_active, created_by, approved_at) VALUES
(1, 'freight_invoice', 1,
 '{"orderId": "OrderID", "courierCode": "CourierCode", "invoiceNo": "InvoiceNo", "invoiceDate": "InvoiceDate", "billedWeight": "BilledWeight", "freightCharge": "FreightCharge", "codFee": "CODFee", "returnFee": "ReturnFee", "totalFreight": "TotalFreight"}',
 true, 'system', CURRENT_DATE);

-- Marketplace 1 Freight Invoice mapping (source_id=2)
INSERT INTO column_mappings (source_id, file_type, version, mappings_json, is_active, created_by, approved_at) VALUES
(2, 'freight_invoice', 1,
 '{"orderId": "OrderID", "courierCode": "CourierCode", "invoiceNo": "InvoiceNo", "invoiceDate": "InvoiceDate", "billedWeight": "BilledWeight", "freightCharge": "FreightCharge", "codFee": "CODFee", "returnFee": "ReturnFee", "totalFreight": "TotalFreight"}',
 true, 'system', CURRENT_DATE);

-- Website Orders mapping (source_id=1)
INSERT INTO column_mappings (source_id, file_type, version, mappings_json, is_active, created_by, approved_at) VALUES
(1, 'orders', 1,
 '{"orderId": "OrderID", "channelCode": "ChannelCode", "orderDate": "OrderDate", "orderValue": "OrderValue", "paymentType": "PaymentType", "deliveryStatus": "DeliveryStatus", "customerName": "CustomerName", "customerPhone": "CustomerPhone", "customerAddress": "CustomerAddress", "expectedAmountOverride": "expected_amount_override"}',
 true, 'system', CURRENT_DATE);

-- Marketplace 1 Orders mapping (source_id=2)
INSERT INTO column_mappings (source_id, file_type, version, mappings_json, is_active, created_by, approved_at) VALUES
(2, 'orders', 1,
 '{"orderId": "OrderID", "channelCode": "ChannelCode", "orderDate": "OrderDate", "orderValue": "OrderValue", "paymentType": "PaymentType", "deliveryStatus": "DeliveryStatus", "customerName": "CustomerName", "customerPhone": "CustomerPhone", "customerAddress": "CustomerAddress", "expectedAmountOverride": "expected_amount_override"}',
 true, 'system', CURRENT_DATE);