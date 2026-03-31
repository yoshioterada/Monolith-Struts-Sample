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
 * SkiShop EC サイトにおける注文の配送先情報を表す JPA エンティティ。
 *
 * <p>{@code order_shipping} テーブルにマッピングされ、注文時に選択された配送先の
 * 受取人名・郵便番号・都道府県・住所・電話番号・配送方法・送料・希望配達日時を保持する。
 * {@link Address} からコピーされたスナップショットであり、注文後のマスタ住所変更の
 * 影響を受けない。</p>
 *
 * <p>主要な関連エンティティ:</p>
 * <ul>
 *   <li>{@link Order} — この配送先が紐づく注文（1:1、{@code order_id} で参照）</li>
 *   <li>{@link ShippingMethod} — 選択された配送方法（{@code shipping_method_code} で参照）</li>
 *   <li>{@link Address} — コピー元のユーザー住所マスタ</li>
 * </ul>
 *
 * @see Order
 * @see ShippingMethod
 * @see Address
 */
@Entity
@Table(name = "order_shipping")
@Getter
@Setter
@NoArgsConstructor
public class OrderShipping {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "order_id", nullable = false, length = 36)
    private String orderId;

    @Column(name = "recipient_name", nullable = false, length = 100)
    private String recipientName;

    @Column(name = "postal_code", nullable = false, length = 20)
    private String postalCode;

    @Column(name = "prefecture", nullable = false, length = 50)
    private String prefecture;

    @Column(name = "address1", nullable = false, length = 200)
    private String address1;

    @Column(name = "address2", length = 200)
    private String address2;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "shipping_method_code", length = 20)
    private String shippingMethodCode;

    @Column(name = "shipping_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal shippingFee;

    @Column(name = "requested_delivery_date")
    private LocalDateTime requestedDeliveryDate;
}
