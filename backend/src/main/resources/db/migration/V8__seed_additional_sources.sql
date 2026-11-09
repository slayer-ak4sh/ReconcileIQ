-- Seed additional sources for marketplaces and couriers
-- Using ON CONFLICT DO NOTHING on unique code
-- partner_type column in database (snake_case), so we use partner_type in SQL

-- Website source (existing)
INSERT INTO sources (name, code, partner_type, channel_code) VALUES ('Website', 'WEBSITE', 'CHANNEL', 'Website')
ON CONFLICT (code) DO NOTHING;

-- Marketplace sources
INSERT INTO sources (name, code, partner_type, channel_code) VALUES ('Amazon', 'AMAZON', 'CHANNEL', 'Website')
ON CONFLICT (code) DO NOTHING;

INSERT INTO sources (name, code, partner_type, channel_code) VALUES ('Flipkart', 'FLIPKART', 'CHANNEL', 'Website')
ON CONFLICT (code) DO NOTHING;

-- Quick-commerce sources
INSERT INTO sources (name, code, partner_type, channel_code) VALUES ('Blinkit', 'BLINKIT', 'CHANNEL', 'Website')
ON CONFLICT (code) DO NOTHING;

INSERT INTO sources (name, code, partner_type, channel_code) VALUES ('Zepto', 'ZEPTO', 'CHANNEL', 'Website')
ON CONFLICT (code) DO NOTHING;

-- Courier sources
INSERT INTO sources (name, code, partner_type, channel_code) VALUES ('Courier A', 'COURIER_A', 'CHANNEL', 'Website')
ON CONFLICT (code) DO NOTHING;

INSERT INTO sources (name, code, partner_type, channel_code) VALUES ('Courier B', 'COURIER_B', 'CHANNEL', 'Website')
ON CONFLICT (code) DO NOTHING;
