-- V2__add_password_prefix.sql
-- Phase 7 で実行: パスワードハッシュに DelegatingPasswordEncoder 用プレフィックスとソルトを付与する
-- 詳細: docs/migration/DESIGN.md §11.2 参照

-- ハッシュとソルトを {sha256}<hash>$<salt> 形式に変換
UPDATE users SET password_hash = CONCAT('{sha256}', password_hash, '$', salt)
WHERE password_hash NOT LIKE '{%}%';
