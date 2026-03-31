-- V8: inventory テーブルに監査用タイムスタンプカラムを追加する。
-- 在庫変更の追跡性を向上させるため、created_at / updated_at を導入する。

ALTER TABLE inventory
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE inventory
    ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
