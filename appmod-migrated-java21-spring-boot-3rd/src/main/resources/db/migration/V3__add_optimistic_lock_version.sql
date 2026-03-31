-- V3: inventory と point_accounts に楽観的ロック用の version カラムを追加する。
-- 並行チェックアウト時の在庫・ポイント更新競合を JPA @Version で検出する。

ALTER TABLE inventory
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE point_accounts
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
