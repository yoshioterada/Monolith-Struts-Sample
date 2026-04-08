-- V12: cart_items テーブルに商品名を保持する非正規化カラムを追加する。
-- カート一覧画面での表示用途。商品テーブルへの JOIN を不要にしパフォーマンスを改善する。

ALTER TABLE cart_items
    ADD COLUMN product_name VARCHAR(255);
