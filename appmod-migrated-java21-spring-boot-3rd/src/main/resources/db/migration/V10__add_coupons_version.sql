-- V10: coupons テーブルに楽観的ロック用の version カラムを追加する。
-- 並行チェックアウト時の used_count 更新で lost update を JPA @Version で防止する。

ALTER TABLE coupons
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
