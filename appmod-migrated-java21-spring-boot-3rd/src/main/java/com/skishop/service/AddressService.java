package com.skishop.service;

import com.skishop.exception.ResourceNotFoundException;
import com.skishop.dto.request.AddressRequest;
import com.skishop.model.Address;
import com.skishop.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 住所管理サービス。
 *
 * <p>ユーザーの配送先住所の CRUD 操作を提供する。
 * チェックアウト時の配送先選択や、アカウント設定画面での住所管理に使用される。</p>
 *
 * <p>依存関係:</p>
 * <ul>
 *   <li>{@link AddressRepository} — 住所エンティティの永続化</li>
 * </ul>
 *
 * @see com.skishop.controller.AccountController
 * @see com.skishop.controller.CheckoutController
 * @see AddressRepository
 */
@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;

    /**
     * 指定ユーザーの全住所を取得する。
     *
     * <p>読み取り専用トランザクションで実行される。</p>
     *
     * @param userId ユーザー ID
     * @return 当該ユーザーに紐づく住所のリスト（存在しない場合は空リスト）
     */
    @Transactional(readOnly = true)
    public List<Address> findByUserId(String userId) {
        return addressRepository.findByUserId(userId);
    }

    /**
     * 住所 ID で住所を取得する。
     *
     * <p>読み取り専用トランザクションで実行される。</p>
     *
     * @param addressId 住所 ID
     * @return 該当する住所エンティティ
     * @throws ResourceNotFoundException 指定 ID の住所が存在しない場合
     */
    @Transactional(readOnly = true)
    public Address findById(String addressId) {
        return addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", addressId));
    }

    /**
     * リクエスト DTO からエンティティを組み立てて保存する。
     *
     * @param request 住所リクエスト DTO
     * @param userId  住所の所有者ユーザー ID
     * @return 保存後の住所エンティティ
     */
    @Transactional
    public Address createFromRequest(AddressRequest request, String userId) {
        var address = new Address();
        address.setId(UUID.randomUUID().toString());
        address.setUserId(userId);
        address.setLabel(request.label());
        address.setRecipientName(request.recipientName());
        address.setPostalCode(request.postalCode());
        address.setPrefecture(request.prefecture());
        address.setAddress1(request.address1());
        address.setAddress2(request.address2());
        address.setPhone(request.phone());
        address.setDefault(request.isDefault());
        return addressRepository.save(address);
    }

    /**
     * 住所を保存（新規作成または更新）する。
     *
     * <p>ID が未設定の場合は UUID を自動生成して新規作成する。
     * 既に ID が設定されている場合は既存レコードの更新となる。</p>
     *
     * @param address 保存対象の住所エンティティ
     * @return 保存後の住所エンティティ（ID が付与された状態）
     */
    @Transactional
    public Address save(Address address) {
        if (address.getId() == null) {
            address.setId(UUID.randomUUID().toString());
        }
        return addressRepository.save(address);
    }

    /**
     * 指定 ID の住所を削除する。
     *
     * @param addressId 削除対象の住所 ID
     */
    @Transactional
    public void delete(String addressId) {
        addressRepository.deleteById(addressId);
    }

    /**
     * 指定 ID の住所を、所有者を検証してから削除する（IDOR 防止）。
     *
     * @param addressId 削除対象の住所 ID
     * @param userId    ログイン中のユーザー ID
     * @throws ResourceNotFoundException 住所が存在しない場合
     * @throws org.springframework.security.access.AccessDeniedException 住所が当該ユーザーのものでない場合
     */
    @Transactional
    public void deleteByIdAndUserId(String addressId, String userId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", addressId));
        if (!address.getUserId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Address does not belong to current user");
        }
        addressRepository.deleteById(addressId);
    }
}
