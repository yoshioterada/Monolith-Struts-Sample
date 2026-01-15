INSERT INTO roles(id, name) VALUES
  ('r-admin', 'ADMIN'),
  ('r-user', 'USER');

INSERT INTO users(id, email, username, password_hash, salt, status, role, created_at, updated_at) VALUES
  ('u-1', 'user@example.com', 'demo', 'hash', 'salt', 'ACTIVE', 'USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO categories(id, name, parent_id) VALUES
  ('c-1', 'Ski', NULL);

INSERT INTO products(id, name, brand, description, category_id, sku, status, created_at, updated_at) VALUES
  ('P001', 'Ski A', 'BrandX', 'Entry ski', 'c-1', 'SKU-001', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO prices(id, product_id, regular_price, sale_price, currency_code, sale_start_date, sale_end_date) VALUES
  ('price-1', 'P001', 50000, NULL, 'JPY', NULL, NULL);

INSERT INTO inventory(id, product_id, quantity, reserved_quantity, status) VALUES
  ('inv-1', 'P001', 10, 0, 'AVAILABLE');

INSERT INTO carts(id, user_id, session_id, status, expires_at) VALUES
  ('cart-1', 'u-1', 'session-1', 'ACTIVE', CURRENT_TIMESTAMP);

INSERT INTO cart_items(id, cart_id, product_id, quantity, unit_price) VALUES
  ('cart-item-1', 'cart-1', 'P001', 1, 50000);

INSERT INTO orders(id, order_number, user_id, status, payment_status, subtotal, tax, shipping_fee, discount_amount, total_amount, coupon_code, used_points, created_at, updated_at) VALUES
  ('order-1', 'ORD-0001', 'u-1', 'CREATED', 'AUTHORIZED', 50000, 5000, 800, 0, 55800, NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO order_items(id, order_id, product_id, product_name, sku, unit_price, quantity, subtotal) VALUES
  ('order-item-1', 'order-1', 'P001', 'Ski A', 'SKU-001', 50000, 1, 50000);

INSERT INTO shipments(id, order_id, carrier, tracking_number, status, shipped_at, delivered_at) VALUES
  ('ship-1', 'order-1', 'Yamato', 'TRK-001', 'PENDING', NULL, NULL);

INSERT INTO returns(id, order_id, order_item_id, reason, quantity, refund_amount, status) VALUES
  ('return-1', 'order-1', 'order-item-1', 'size', 1, 50000, 'REQUESTED');

INSERT INTO point_accounts(id, user_id, balance, lifetime_earned, lifetime_redeemed) VALUES
  ('pa-1', 'u-1', 100, 100, 0);

INSERT INTO point_transactions(id, user_id, type, amount, reference_id, description, expires_at, is_expired, created_at) VALUES
  ('pt-1', 'u-1', 'EARN', 100, 'order-1', 'Initial points', CURRENT_TIMESTAMP, FALSE, CURRENT_TIMESTAMP);

INSERT INTO campaigns(id, name, description, type, start_date, end_date, is_active, rules_json) VALUES
  ('camp-1', 'Winter', 'Winter campaign', 'SEASONAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, TRUE, '{}');

INSERT INTO coupons(id, campaign_id, code, coupon_type, discount_value, discount_type, minimum_amount, maximum_discount, usage_limit, used_count, is_active, expires_at) VALUES
  ('coupon-1', 'camp-1', 'SAVE10', 'PERCENT', 10, 'ORDER', 1000, 5000, 100, 0, TRUE, CURRENT_TIMESTAMP);

INSERT INTO user_addresses(id, user_id, label, recipient_name, postal_code, prefecture, address1, address2, phone, is_default, created_at, updated_at) VALUES
  ('addr-1', 'u-1', '自宅', '山田 太郎', '160-0022', '東京都', '新宿区新宿1-1-1', 'マンション101', '0312345678', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO password_reset_tokens(id, user_id, token, expires_at, used_at) VALUES
  ('prt-1', 'u-1', 'token-1', CURRENT_TIMESTAMP, NULL);

INSERT INTO shipping_methods(id, code, name, fee, is_active, sort_order) VALUES
  ('ship-std', 'STANDARD', 'Standard', 800, TRUE, 1);

INSERT INTO email_queue(id, to_addr, subject, body, status, retry_count, last_error, scheduled_at, sent_at) VALUES
  ('mail-1', 'user@example.com', 'Welcome', 'Body', 'PENDING', 0, NULL, CURRENT_TIMESTAMP, NULL);
