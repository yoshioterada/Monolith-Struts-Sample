package com.skishop.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SkiShop EC サイトにおけるユーザーのポイント口座を表す JPA エンティティ。
 *
 * <p>{@code point_accounts} テーブルにマッピングされ、ユーザーごとのポイント残高
 * （{@code balance}）・累計獲得ポイント（{@code lifetime_earned}）・
 * 累計使用ポイント（{@code lifetime_redeemed}）を保持する。
 * ユーザーと 1:1 の関係を持つ。</p>
 *
 * <p>チェックアウト処理ではポイントの仮消費（{@code reservePoints}）→
 * ポイント確定付与（{@code awardPoints}）の順で操作される。</p>
 *
 * <p>主要な関連エンティティ:</p>
 * <ul>
 *   <li>{@link User} — このポイント口座を所有するユーザー（1:1、{@code user_id} で参照）</li>
 *   <li>{@link PointTransaction} — ポイントの獲得・使用・失効の取引履歴（1:N）</li>
 * </ul>
 *
 * @see User
 * @see PointTransaction
 */
@Entity
@Table(name = "point_accounts")
@Getter
@Setter
@NoArgsConstructor
public class PointAccount {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "balance", nullable = false)
    private int balance;

    @Column(name = "lifetime_earned", nullable = false)
    private int lifetimeEarned;

    @Column(name = "lifetime_redeemed", nullable = false)
    private int lifetimeRedeemed;
}
