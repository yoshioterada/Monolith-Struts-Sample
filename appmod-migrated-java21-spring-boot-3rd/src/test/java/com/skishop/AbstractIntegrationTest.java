package com.skishop;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * 統合テスト共通基底クラス。
 * 各テストはトランザクション内で実行され、テスト後に自動ロールバックされる。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public abstract class AbstractIntegrationTest {
}
