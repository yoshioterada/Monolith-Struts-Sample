-- V9: orders テーブルに楽観的ロック用の version カラムを追加する。
-- 注文ステータス更新（キャンセル・返品等）の並行操作競合を JPA @Version で検出する。

ALTER TABLE orders
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
