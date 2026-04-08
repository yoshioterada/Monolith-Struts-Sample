-- V13: users, products, carts テーブルに楽観的ロック用の version カラムを追加する。
-- 並行更新（プロフィール変更、商品管理、カート操作）の競合を JPA @Version で検出する。

ALTER TABLE users ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE products ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE carts ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
