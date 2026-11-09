-- Flyway Migration V9: Seed at least 8 required sources
INSERT INTO sources (name, code, partner_type, channel_code) VALUES ('Website', 'WEBSITE', 'CHANNEL', 'Website')
ON CONFLICT (code) DO NOTHING;

INSERT INTO sources (name, code, partner_type, channel_code) VALUES ('Marketplace 1', 'MP1', 'CHANNEL', 'Marketplace 1')
ON CONFLICT (code) DO NOTHING;

INSERT INTO sources (name, code, partner_type, channel_code) VALUES ('Amazon', 'AMAZON', 'CHANNEL', 'Amazon')
ON CONFLICT (code) DO NOTHING;

INSERT INTO sources (name, code, partner_type, channel_code) VALUES ('Flipkart', 'FLIPKART', 'CHANNEL', 'Flipkart')
ON CONFLICT (code) DO NOTHING;

INSERT INTO sources (name, code, partner_type, channel_code) VALUES ('Blinkit', 'BLINKIT', 'CHANNEL', 'Blinkit')
ON CONFLICT (code) DO NOTHING;

INSERT INTO sources (name, code, partner_type, channel_code) VALUES ('Zepto', 'ZEPTO', 'CHANNEL', 'Zepto')
ON CONFLICT (code) DO NOTHING;

INSERT INTO sources (name, code, partner_type, courier_code) VALUES ('Courier A', 'COURIER_A', 'COURIER', 'Courier A')
ON CONFLICT (code) DO NOTHING;

INSERT INTO sources (name, code, partner_type, courier_code) VALUES ('Courier B', 'COURIER_B', 'COURIER', 'Courier B')
ON CONFLICT (code) DO NOTHING;
