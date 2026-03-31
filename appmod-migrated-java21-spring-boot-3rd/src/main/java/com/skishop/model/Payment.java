package com.skishop.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SkiShop EC サイトにおける支払い情報を表す JPA エンティティ。
 *
 * <p>{@code payments} テーブルにマッピングされ、支払い金額・通貨・決済ステータス・
 * 決済インテント ID（外部決済サービスとの連携用）・作成日時を保持する。
 * 注文確定フローのステップ 8 で作成され、注文（{@code order_id}）または
 * カート（{@code cart_id}）に紐づく。</p>
 *
 * <p>決済ステータス遷移: {@code PENDING} → {@code COMPLETED} / {@code FAILED} / {@code REFUNDED}</p>
 *
 * <p>主要な関連エンティティ:</p>
 * <ul>
 *   <li>{@link Order} — この支払いが紐づく注文（N:1、{@code order_id} で参照）</li>
 *   <li>{@link Cart} — 仮決済時のカート参照（N:1、{@code cart_id} で参照）</li>
 * </ul>
 *
 * @see Order
 * @see Cart
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
public class Payment {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "order_id", length = 36)
    private String orderId;

    @Column(name = "cart_id", length = 36)
    private String cartId;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "payment_intent_id", length = 100)
    private String paymentIntentId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
