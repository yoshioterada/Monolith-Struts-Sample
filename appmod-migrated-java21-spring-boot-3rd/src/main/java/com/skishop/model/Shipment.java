package com.skishop.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * SkiShop EC サイトにおける出荷情報を表す JPA エンティティ。
 *
 * <p>{@code shipments} テーブルにマッピングされ、注文に対する出荷の配送業者・
 * 追跡番号・出荷ステータス・出荷日時・配達完了日時を保持する。
 * 注文の出荷状況を追跡するために利用され、追跡番号によりユーザーが
 * 配送状況を確認できる。</p>
 *
 * <p>出荷ステータス遷移: {@code PREPARING} → {@code SHIPPED} → {@code DELIVERED}</p>
 *
 * <p>主要な関連エンティティ:</p>
 * <ul>
 *   <li>{@link Order} — この出荷が紐づく注文（N:1、{@code order_id} で参照）</li>
 * </ul>
 *
 * @see Order
 */
@Entity
@Table(name = "shipments")
@Getter
@Setter
@NoArgsConstructor
public class Shipment {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "order_id", nullable = false, length = 36)
    private String orderId;

    @Column(name = "carrier", length = 100)
    private String carrier;

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;
}
