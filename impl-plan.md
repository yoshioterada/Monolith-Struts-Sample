# 実装計画書 (SkiShop Monolith - Struts 1.2.9 / Java 5)

<!-- markdownlint-disable MD013 MD029 -->

## フェーズ構成

0. 環境・骨組み
1. ドメイン/DAO/DDL
2. サービス/ビジネスロジック
3. Web層(Action/Form/Validation)
4. View(JSP/Tiles)
5. セキュリティ/RequestProcessor
6. 管理機能(Admin)
7. 通知/メール
8. 非機能/運用/Docker
9. テスト/QA
10. リリース

---

## Phase 0: 環境・骨組み

**内容**

- Maven 2 プロジェクト雛形作成（`source/target=1.5`）。
- パッケージ構造/フォルダ構成作成（`src/main/java`, `src/main/webapp`）。
- `web.xml` / `struts-config.xml` スケルトン、`context.xml`、`log4j.properties` 雛形。
- `pom.xml` 依存追加（Struts 1.2.9, log4j, dbcp, dbutils, javax.mail）。
- `.editorconfig` / コーディング規約メモ。

**Exit Criteria**

- `mvn -B -DskipTests package` が成功し、空の WAR が生成される。
- Tomcat (6 or 8) で ActionServlet が起動し 404 以外のエラーなく起動ログが出る。

---

## Phase 1: ドメイン/DAO/DDL

**内容**

- ドメイン/DTO 定義：User, Role, Product, Category, Price, Inventory, Cart, Order, OrderItem, Shipment, Return, PointAccount, PointTransaction, Coupon, Campaign, Address, PasswordResetToken, ShippingMethod, EmailQueue。
- `AbstractDao`, `DaoException`, `DataSourceLocator` 実装。
- DAO インタフェースと実装作成（UserDao, ProductDao, InventoryDao, CouponDao, CartDao, OrderDao, PointAccountDao, PointTransactionDao, UserAddressDao, PasswordResetTokenDao, ShippingMethodDao, EmailQueueDao）。
- DDL スクリプト作成（全テーブル・インデックス・制約）。
- 初期データ投入 SQL（サンプル商品・ユーザ・住所・在庫）。

**Exit Criteria**

- H2/PG で DDL 実行成功、DAO CRUD テストが JUnit + H2 でグリーン。
- 主要 DAO のメソッドが Null を返さず期待データを返す（findByEmail, findPaged, reserve など）。

---

## Phase 2: サービス/ビジネスロジック

**内容**

- サービス実装：AuthService, UserService, ProductService, InventoryService, CouponService, CartService, PaymentService(擬似), OrderService, PointService, ShippingService, TaxService, MailService。
- `OrderFacade` 実装（placeOrder, cancelOrder, returnOrder）。
- ビジネスルール：
  - クーポン検証（usage_limit, minimum_amount,期間,is_active）。
  - ポイント付与/利用（1%・365日）
  - 税計算10%、送料800円/1万円以上無料。
  - 在庫悲観ロック `SELECT ... FOR UPDATE`、不足時例外。
  - 決済オーソリ成功/失敗ハンドリング、キャンセル/返品時ポイント・クーポン調整。

**Exit Criteria**

- JUnit サービステスト（H2 + DBUnit）がグリーン：
  - 成功: checkout (coupon+points) → order/point/coupon_usage/stock 更新。
  - キャンセル: 在庫戻し・ポイント減算・クーポン使用数減算。
  - 返品: 返金レコード・ポイント調整。

---

## Phase 3: Web層 (Action/Form/Validation)

**内容**

- Action 実装：Login, Register, PasswordForgot, PasswordReset, ProductList, ProductDetail, Cart, Checkout, CouponApply, OrderHistory, OrderCancel, OrderReturn, PointBalance, AddressList, AddressSave, Logout。
- ActionForm 実装：LoginForm, RegisterForm, PasswordResetRequestForm, PasswordResetForm, ProductSearchForm, AddCartForm, CheckoutForm, CouponForm, AddressForm, AdminProductForm。
- `validation.xml` ルール整備（メール、パスワード、数量、郵便番号、電話、カード情報）。
- `messages.properties` キー整備。

**Exit Criteria**

- StrutsTestCase で主要 Action のフォワード/バリデーションが期待通り（success/failure）。
- `validation.xml` に基づき入力エラー表示が JSP で確認できる。

---

## Phase 4: View (JSP/Tiles)

**内容**

- Tiles レイアウト `base.jsp`、共通 `header.jsp`, `footer.jsp`, `messages.jsp`。
- JSP 作成：auth/login, auth/register, auth/password/forgot/reset, products/list/detail, cart/view/checkout/confirmation, orders/history/detail, points/balance, account/addresses/address_edit, admin/products/list/edit, admin/orders/list/detail。
- Strutsタグ（html/bean/logic）適用、`<bean:write filter="true"/>` デフォルト。
- CSRF トークン埋め込み（`<html:hidden property="org.apache.struts.taglib.html.TOKEN"/>`）。

**Exit Criteria**

- 手動動作確認: Tomcat 上でページがレンダリングされ、タグ解決エラーなし。
- カート追加～チェックアウトまで UI 経路が完走。

---

## Phase 5: セキュリティ / RequestProcessor

**内容**

- `AuthRequestProcessor` 拡張：認可（roles）、未認証リダイレクト、CSRF 検証、セッション固定化対策（ログイン時 invalidate→新規）。
- ログイン試行制限（5回、`security_logs` 記録、`users.status=LOCKED`）。
- パスワードハッシュ（SHA-256 + salt + 1000 iterations）。
- Cookie 設定 `CART_ID` HttpOnly/Secure。

**Exit Criteria**

- 未ログインで保護リソースアクセス→`/login.do` リダイレクト。
- CSRF 不正時 403 相当レスポンス。
- 5回誤りでロック、ログ記録確認。

---

## Phase 6: 管理機能 (Admin)

**内容**

- Admin Actions/Forms/JSP: 商品 CRUD, 在庫更新, 注文ステータス更新・返金, クーポン管理(任意), 配送方法管理(任意)。
- 管理者ロール判定。

**Exit Criteria**

- 管理者ユーザでログインし、商品一覧/編集・在庫更新・注文更新が可能。
- 非管理者は `/admin/*` へアクセス不可。

---

## Phase 7: 通知 / メール

**内容**

- `MailService` 実装（JavaMail 1.4.7）、SMTP 設定読込。
- `email_queue` DAO と送信ジョブ（シンプルスレッド or Timer）。
- テンプレート（注文確定・パスワードリセット）を JSP/Velocity なしのシンプル文字列で用意。

**Exit Criteria**

- ローカル SMTP（MailHog 等）でメール送信確認。
- 失敗時リトライ・`status/retry_count/last_error` 更新。

---

## Phase 8: 非機能 / 運用 / Docker

**内容**

- log4j MDC (`reqId`)、RollingFileAppender 設定。
- `app.properties` ロードと ServiceLocator/Factory 連携。
- DBCP チューニング（maxActive/maxIdle/maxWait）。
- `Dockerfile`(Tomcat8/JDK8)・`Dockerfile.tomcat6`・`docker-compose.yml`(PG) 設定。
- `.dockerignore` 整備。

**Exit Criteria**

- `docker-compose up` でアプリ+DB 起動、基本フロー成功。
- WAR サイズとログローテーションが期待通り。

---

## Phase 9: テスト / QA

**内容**

- StrutsTestCase: Actions カバレッジ。
- JUnit + DBUnit: DAO/Service カバレッジ。
- シナリオテスト: 登録→ログイン→検索→カート→チェックアウト→キャンセル→返品。
- 負荷テスト (任意) JMeter/Locust で簡易チェック。

**Exit Criteria**

- テストスイート緑、カバレッジ (Routes/Services/DAO) 80% 目標。
- 回帰テストチェックリスト完了。

---

## Phase 10: リリース

**内容**

- WAR バージョニング、リリースノート作成。
- デプロイ手順書 (Tomcat 6/8)、環境変数とコンテキスト設定整理。
- バックアップ/リストア手順 (DB) メモ。

**Exit Criteria**

- Tomcat6/Tomcat8 へのデプロイ実施・動作確認。
- ドキュメント更新（README/実装計画/運用手順）。

---

## 補足 (クロスカット)

- コーディング規約準拠（Java 1.5 構文、ジェネリクス任意、注釈なし）。
- レビュー観点チェックリスト（SQLインジェクション、Nullチェック、ロギング、例外変換）。
- Lint/Checkstyle (Java 5 設定) 任意導入。
- i18n: `messages.properties` / `messages_ja.properties`。
- Runbook: スレッドダンプ取得、ログローテーション確認手順。
