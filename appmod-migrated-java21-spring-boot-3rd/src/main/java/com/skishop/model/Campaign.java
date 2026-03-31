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
 * SkiShop EC サイトにおけるキャンペーン（販促施策）を表す JPA エンティティ。
 *
 * <p>{@code campaigns} テーブルにマッピングされ、キャンペーンの名称・説明・種別・
 * 開催期間（開始日・終了日）・有効フラグ・適用ルール（JSON 形式）を保持する。
 * セールイベントやポイント倍率アップなどの販促施策を管理する。</p>
 *
 * <p>主要な関連エンティティ:</p>
 * <ul>
 *   <li>{@link Coupon} — このキャンペーンに紐づくクーポン（1:N、{@code campaign_id} で参照）</li>
 * </ul>
 *
 * @see Coupon
 */
@Entity
@Table(name = "campaigns")
@Getter
@Setter
@NoArgsConstructor
public class Campaign {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "type", length = 50)
    private String type;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "rules_json", length = 2000)
    private String rulesJson;
}
