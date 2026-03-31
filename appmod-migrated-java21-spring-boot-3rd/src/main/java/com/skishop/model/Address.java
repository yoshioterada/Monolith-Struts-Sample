package com.skishop.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * SkiShop EC サイトにおけるユーザーの配送先住所を表す JPA エンティティ。
 *
 * <p>{@code user_addresses} テーブルにマッピングされ、受取人名・郵便番号・都道府県・
 * 住所・電話番号などの配送に必要な情報を保持する。
 * 各住所にはラベル（「自宅」「勤務先」等）を設定でき、デフォルト住所フラグにより
 * ユーザーの優先配送先を識別する。</p>
 *
 * <p>主要な関連エンティティ:</p>
 * <ul>
 *   <li>{@link User} — この住所を所有するユーザー（N:1、{@code user_id} で参照）</li>
 *   <li>{@link OrderShipping} — 注文時にこの住所情報がコピーされる</li>
 * </ul>
 *
 * @see User
 * @see OrderShipping
 */
@Entity
@Table(name = "user_addresses")
@Getter
@Setter
@NoArgsConstructor
public class Address {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "label", nullable = false, length = 50)
    private String label;

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

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
