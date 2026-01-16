CREATE TABLE roles (
  id VARCHAR(36) PRIMARY KEY,
  name VARCHAR(50) NOT NULL
);

CREATE TABLE users (
  id VARCHAR(36) PRIMARY KEY,
  email VARCHAR(255) NOT NULL,
  username VARCHAR(100) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  salt VARCHAR(255) NOT NULL,
  status VARCHAR(20) NOT NULL,
  role VARCHAR(20) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX idx_users_email ON users(email);

CREATE TABLE security_logs (
  id VARCHAR(36) PRIMARY KEY,
  user_id VARCHAR(36),
  event_type VARCHAR(50) NOT NULL,
  ip_address VARCHAR(50),
  user_agent VARCHAR(255),
  details_json VARCHAR(2000)
);

CREATE INDEX idx_security_logs_user_event ON security_logs(user_id, event_type);

CREATE TABLE categories (
  id VARCHAR(36) PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  parent_id VARCHAR(36)
);

CREATE TABLE products (
  id VARCHAR(20) PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  brand VARCHAR(100),
  description VARCHAR(2000),
  category_id VARCHAR(36),
  sku VARCHAR(100),
  status VARCHAR(20) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_products_category ON products(category_id);

CREATE TABLE prices (
  id VARCHAR(36) PRIMARY KEY,
  product_id VARCHAR(20) NOT NULL,
  regular_price DECIMAL(10,2) NOT NULL,
  sale_price DECIMAL(10,2),
  currency_code VARCHAR(10) NOT NULL,
  sale_start_date TIMESTAMP,
  sale_end_date TIMESTAMP
);

CREATE TABLE inventory (
  id VARCHAR(36) PRIMARY KEY,
  product_id VARCHAR(20) NOT NULL,
  quantity INT NOT NULL,
  reserved_quantity INT NOT NULL,
  status VARCHAR(20) NOT NULL
);

CREATE UNIQUE INDEX idx_inventory_product ON inventory(product_id);

CREATE TABLE carts (
  id VARCHAR(36) PRIMARY KEY,
  user_id VARCHAR(36),
  session_id VARCHAR(100),
  status VARCHAR(20) NOT NULL,
  expires_at TIMESTAMP
);

CREATE TABLE cart_items (
  id VARCHAR(36) PRIMARY KEY,
  cart_id VARCHAR(36) NOT NULL,
  product_id VARCHAR(20) NOT NULL,
  quantity INT NOT NULL,
  unit_price DECIMAL(10,2) NOT NULL
);

CREATE UNIQUE INDEX idx_cart_items_cart_product ON cart_items(cart_id, product_id);

CREATE TABLE payments (
  id VARCHAR(36) PRIMARY KEY,
  order_id VARCHAR(36),
  cart_id VARCHAR(36),
  amount DECIMAL(10,2) NOT NULL,
  currency VARCHAR(10) NOT NULL,
  status VARCHAR(20) NOT NULL,
  payment_intent_id VARCHAR(100),
  created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_payments_order ON payments(order_id);

CREATE TABLE orders (
  id VARCHAR(36) PRIMARY KEY,
  order_number VARCHAR(50) NOT NULL,
  user_id VARCHAR(36),
  status VARCHAR(20) NOT NULL,
  payment_status VARCHAR(20) NOT NULL,
  subtotal DECIMAL(10,2) NOT NULL,
  tax DECIMAL(10,2) NOT NULL,
  shipping_fee DECIMAL(10,2) NOT NULL,
  discount_amount DECIMAL(10,2) NOT NULL,
  total_amount DECIMAL(10,2) NOT NULL,
  coupon_code VARCHAR(50),
  used_points INT NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX idx_orders_order_number ON orders(order_number);
CREATE INDEX idx_orders_user ON orders(user_id);

CREATE TABLE order_items (
  id VARCHAR(36) PRIMARY KEY,
  order_id VARCHAR(36) NOT NULL,
  product_id VARCHAR(20) NOT NULL,
  product_name VARCHAR(255) NOT NULL,
  sku VARCHAR(100),
  unit_price DECIMAL(10,2) NOT NULL,
  quantity INT NOT NULL,
  subtotal DECIMAL(10,2) NOT NULL
);

CREATE INDEX idx_order_items_order ON order_items(order_id);

CREATE TABLE shipments (
  id VARCHAR(36) PRIMARY KEY,
  order_id VARCHAR(36) NOT NULL,
  carrier VARCHAR(100),
  tracking_number VARCHAR(100),
  status VARCHAR(20),
  shipped_at TIMESTAMP,
  delivered_at TIMESTAMP
);

CREATE TABLE returns (
  id VARCHAR(36) PRIMARY KEY,
  order_id VARCHAR(36) NOT NULL,
  order_item_id VARCHAR(36) NOT NULL,
  reason VARCHAR(255),
  quantity INT NOT NULL,
  refund_amount DECIMAL(10,2) NOT NULL,
  status VARCHAR(20)
);

CREATE TABLE order_shipping (
  id VARCHAR(36) PRIMARY KEY,
  order_id VARCHAR(36) NOT NULL,
  recipient_name VARCHAR(100) NOT NULL,
  postal_code VARCHAR(20) NOT NULL,
  prefecture VARCHAR(50) NOT NULL,
  address1 VARCHAR(200) NOT NULL,
  address2 VARCHAR(200),
  phone VARCHAR(20),
  shipping_method_code VARCHAR(20),
  shipping_fee DECIMAL(10,2) NOT NULL,
  requested_delivery_date TIMESTAMP
);

CREATE TABLE point_accounts (
  id VARCHAR(36) PRIMARY KEY,
  user_id VARCHAR(36) NOT NULL,
  balance INT NOT NULL,
  lifetime_earned INT NOT NULL,
  lifetime_redeemed INT NOT NULL
);

CREATE TABLE point_transactions (
  id VARCHAR(36) PRIMARY KEY,
  user_id VARCHAR(36) NOT NULL,
  type VARCHAR(20) NOT NULL,
  amount INT NOT NULL,
  reference_id VARCHAR(50),
  description VARCHAR(255),
  expires_at TIMESTAMP,
  is_expired BOOLEAN NOT NULL,
  created_at TIMESTAMP NOT NULL
);

CREATE TABLE campaigns (
  id VARCHAR(36) PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  description VARCHAR(1000),
  type VARCHAR(50),
  start_date TIMESTAMP,
  end_date TIMESTAMP,
  is_active BOOLEAN NOT NULL,
  rules_json VARCHAR(2000)
);

CREATE TABLE coupons (
  id VARCHAR(36) PRIMARY KEY,
  campaign_id VARCHAR(36),
  code VARCHAR(50) NOT NULL,
  coupon_type VARCHAR(20) NOT NULL,
  discount_value DECIMAL(10,2) NOT NULL,
  discount_type VARCHAR(20) NOT NULL,
  minimum_amount DECIMAL(10,2),
  maximum_discount DECIMAL(10,2),
  usage_limit INT NOT NULL,
  used_count INT NOT NULL,
  is_active BOOLEAN NOT NULL,
  expires_at TIMESTAMP
);

CREATE UNIQUE INDEX idx_coupons_code ON coupons(code);

CREATE TABLE coupon_usage (
  id VARCHAR(36) PRIMARY KEY,
  coupon_id VARCHAR(36) NOT NULL,
  user_id VARCHAR(36) NOT NULL,
  order_id VARCHAR(36) NOT NULL,
  discount_applied DECIMAL(10,2) NOT NULL,
  used_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_coupon_usage_coupon ON coupon_usage(coupon_id);
CREATE INDEX idx_coupon_usage_order ON coupon_usage(order_id);

CREATE TABLE user_addresses (
  id VARCHAR(36) PRIMARY KEY,
  user_id VARCHAR(36) NOT NULL,
  label VARCHAR(50) NOT NULL,
  recipient_name VARCHAR(100) NOT NULL,
  postal_code VARCHAR(20) NOT NULL,
  prefecture VARCHAR(50) NOT NULL,
  address1 VARCHAR(200) NOT NULL,
  address2 VARCHAR(200),
  phone VARCHAR(20),
  is_default BOOLEAN NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_user_addresses_user ON user_addresses(user_id);

CREATE TABLE password_reset_tokens (
  id VARCHAR(36) PRIMARY KEY,
  user_id VARCHAR(36) NOT NULL,
  token VARCHAR(100) NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  used_at TIMESTAMP
);

CREATE UNIQUE INDEX idx_password_reset_tokens_token ON password_reset_tokens(token);

CREATE TABLE shipping_methods (
  id VARCHAR(36) PRIMARY KEY,
  code VARCHAR(20) NOT NULL,
  name VARCHAR(100) NOT NULL,
  fee DECIMAL(10,2) NOT NULL,
  is_active BOOLEAN NOT NULL,
  sort_order INT NOT NULL
);

CREATE UNIQUE INDEX idx_shipping_methods_code ON shipping_methods(code);

CREATE TABLE email_queue (
  id VARCHAR(36) PRIMARY KEY,
  to_addr VARCHAR(255) NOT NULL,
  subject VARCHAR(255) NOT NULL,
  body VARCHAR(4000) NOT NULL,
  status VARCHAR(20) NOT NULL,
  retry_count INT NOT NULL,
  last_error VARCHAR(2000),
  scheduled_at TIMESTAMP NOT NULL,
  sent_at TIMESTAMP
);

CREATE INDEX idx_email_queue_status ON email_queue(status);
