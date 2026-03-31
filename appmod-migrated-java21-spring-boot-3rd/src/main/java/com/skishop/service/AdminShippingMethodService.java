package com.skishop.service;

import com.skishop.dto.request.admin.AdminShippingMethodRequest;
import com.skishop.exception.ResourceNotFoundException;
import com.skishop.model.ShippingMethod;
import com.skishop.repository.ShippingMethodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 管理者向け配送方法管理サービス。
 *
 * <p>配送方法（通常配送・速達・店舗受取等）の CRUD 操作を管理者に提供する。
 * 全ての変更操作（作成・更新・削除）は {@code ADMIN} ロールを持つユーザーのみ実行可能。</p>
 *
 * <p>依存関係:</p>
 * <ul>
 *   <li>{@link ShippingMethodRepository} — 配送方法エンティティの永続化</li>
 * </ul>
 *
 * @see com.skishop.controller.AdminController
 * @see ShippingMethodRepository
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminShippingMethodService {

    private final ShippingMethodRepository shippingMethodRepository;

    /**
     * 全配送方法を取得する。
     *
     * <p>読み取り専用トランザクションで実行される。有効・無効を問わず全件返す。</p>
     *
     * @return 全配送方法のリスト
     */
    @Transactional(readOnly = true)
    public List<ShippingMethod> listAll() {
        return shippingMethodRepository.findAll();
    }

    /**
     * 配送方法 ID で配送方法を取得する。
     *
     * <p>読み取り専用トランザクションで実行される。</p>
     *
     * @param id 配送方法 ID
     * @return 該当する配送方法エンティティ
     * @throws ResourceNotFoundException 指定 ID の配送方法が存在しない場合
     */
    @Transactional(readOnly = true)
    public ShippingMethod findById(String id) {
        return shippingMethodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ShippingMethod", id));
    }

    /**
     * 新しい配送方法を作成する。
     *
     * <p>{@code ADMIN} ロールが必要。UUID を自動生成して新規レコードを作成する。</p>
     *
     * @param request 配送方法の作成リクエスト（コード、名前、料金、有効フラグ、表示順）
     * @return 作成された配送方法エンティティ
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ShippingMethod create(AdminShippingMethodRequest request) {
        var method = new ShippingMethod();
        method.setId(UUID.randomUUID().toString());
        populateMethod(method, request);
        return shippingMethodRepository.save(method);
    }

    /**
     * 既存の配送方法を更新する。
     *
     * <p>{@code ADMIN} ロールが必要。指定 ID の配送方法が存在しない場合は例外をスローする。</p>
     *
     * @param id      更新対象の配送方法 ID
     * @param request 更新内容を含むリクエスト
     * @return 更新後の配送方法エンティティ
     * @throws ResourceNotFoundException 指定 ID の配送方法が存在しない場合
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ShippingMethod update(String id, AdminShippingMethodRequest request) {
        var method = shippingMethodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ShippingMethod", id));
        populateMethod(method, request);
        return shippingMethodRepository.save(method);
    }

    /**
     * 配送方法を削除する。
     *
     * <p>{@code ADMIN} ロールが必要。指定 ID の配送方法が存在しない場合は例外をスローする。</p>
     *
     * @param id 削除対象の配送方法 ID
     * @throws ResourceNotFoundException 指定 ID の配送方法が存在しない場合
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void delete(String id) {
        var method = shippingMethodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ShippingMethod", id));
        shippingMethodRepository.delete(method);
    }

    private void populateMethod(ShippingMethod method, AdminShippingMethodRequest request) {
        method.setCode(request.code());
        method.setName(request.name());
        method.setFee(request.fee());
        method.setActive(request.active());
        method.setSortOrder(request.sortOrder());
    }
}
