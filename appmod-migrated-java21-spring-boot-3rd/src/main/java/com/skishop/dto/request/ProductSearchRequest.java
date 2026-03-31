package com.skishop.dto.request;

import jakarta.validation.constraints.Min;

/**
 * 商品検索リクエストを表すデータ転送オブジェクト（DTO）。
 *
 * <p>商品一覧画面でのキーワード検索・カテゴリ絞り込み・ページネーション・
 * ソート条件を保持する。
 * Struts 移行元: {@code ProductSearchForm}（{@code ValidatorForm} 継承）</p>
 *
 * <p>バリデーション規則:</p>
 * <ul>
 *   <li>{@code keyword} — 任意（キーワード未指定時は全件対象）</li>
 *   <li>{@code categoryId} — 任意（カテゴリ未指定時は全カテゴリ対象）</li>
 *   <li>{@code page} — 最小値 1</li>
 *   <li>{@code size} — 最小値 1</li>
 *   <li>{@code sort} — 任意（ソート条件、例: {@code "price_asc"}, {@code "name_desc"}）</li>
 * </ul>
 *
 * @param keyword    検索キーワード（商品名・説明文を対象に部分一致検索）
 * @param categoryId 絞り込み対象のカテゴリ ID
 * @param page       ページ番号（1 始まり）
 * @param size       1 ページあたりの表示件数
 * @param sort       ソート条件（例: {@code "price_asc"}, {@code "created_at_desc"}）
 * @see com.skishop.controller.ProductController
 */
public record ProductSearchRequest(
    String keyword,
    String categoryId,
    @Min(1) int page,
    @Min(1) int size,
    String sort
) {}
