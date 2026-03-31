-- V4: 主要テーブルに外部キー制約を追加する。
-- スキーマの参照整合性を DB レベルで保証し、孤立レコードの混入を防ぐ。

-- categories: 親カテゴリへの自己参照 FK
ALTER TABLE categories
    ADD CONSTRAINT fk_categories_parent
        FOREIGN KEY (parent_id) REFERENCES categories(id);

-- products: カテゴリへの FK
ALTER TABLE products
    ADD CONSTRAINT fk_products_category
        FOREIGN KEY (category_id) REFERENCES categories(id);

-- prices: 商品への FK
ALTER TABLE prices
    ADD CONSTRAINT fk_prices_product
        FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE;

-- inventory: 商品への FK
ALTER TABLE inventory
    ADD CONSTRAINT fk_inventory_product
        FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE;

-- carts: ユーザーへの FK（ゲストカートは NULL 許容のため RESTRICT）
ALTER TABLE carts
    ADD CONSTRAINT fk_carts_user
        FOREIGN KEY (user_id) REFERENCES users(id);

-- cart_items: カートへの FK
ALTER TABLE cart_items
    ADD CONSTRAINT fk_cart_items_cart
        FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE;

-- cart_items: 商品への FK
ALTER TABLE cart_items
    ADD CONSTRAINT fk_cart_items_product
        FOREIGN KEY (product_id) REFERENCES products(id);

-- orders: ユーザーへの FK（ゲスト注文は NULL 許容）
ALTER TABLE orders
    ADD CONSTRAINT fk_orders_user
        FOREIGN KEY (user_id) REFERENCES users(id);

-- order_items: 注文への FK
ALTER TABLE order_items
    ADD CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE;

-- order_shipping: 注文への FK
ALTER TABLE order_shipping
    ADD CONSTRAINT fk_order_shipping_order
        FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE;

-- payments: 注文への FK（NULL 許容: 与信前の payment レコード）
ALTER TABLE payments
    ADD CONSTRAINT fk_payments_order
        FOREIGN KEY (order_id) REFERENCES orders(id);

-- shipments: 注文への FK
ALTER TABLE shipments
    ADD CONSTRAINT fk_shipments_order
        FOREIGN KEY (order_id) REFERENCES orders(id);

-- returns: 注文への FK
ALTER TABLE returns
    ADD CONSTRAINT fk_returns_order
        FOREIGN KEY (order_id) REFERENCES orders(id);

-- returns: 注文明細への FK
ALTER TABLE returns
    ADD CONSTRAINT fk_returns_order_item
        FOREIGN KEY (order_item_id) REFERENCES order_items(id);

-- point_accounts: ユーザーへの FK
ALTER TABLE point_accounts
    ADD CONSTRAINT fk_point_accounts_user
        FOREIGN KEY (user_id) REFERENCES users(id);

-- point_transactions: ユーザーへの FK
ALTER TABLE point_transactions
    ADD CONSTRAINT fk_point_transactions_user
        FOREIGN KEY (user_id) REFERENCES users(id);

-- coupons: キャンペーンへの FK（NULL 許容: キャンペーン外クーポン）
ALTER TABLE coupons
    ADD CONSTRAINT fk_coupons_campaign
        FOREIGN KEY (campaign_id) REFERENCES campaigns(id);

-- coupon_usage: クーポンへの FK
ALTER TABLE coupon_usage
    ADD CONSTRAINT fk_coupon_usage_coupon
        FOREIGN KEY (coupon_id) REFERENCES coupons(id);

-- coupon_usage: 注文への FK
ALTER TABLE coupon_usage
    ADD CONSTRAINT fk_coupon_usage_order
        FOREIGN KEY (order_id) REFERENCES orders(id);

-- user_addresses: ユーザーへの FK
ALTER TABLE user_addresses
    ADD CONSTRAINT fk_user_addresses_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- password_reset_tokens: ユーザーへの FK
ALTER TABLE password_reset_tokens
    ADD CONSTRAINT fk_password_reset_tokens_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
