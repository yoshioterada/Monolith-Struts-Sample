package com.skishop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * SkiShop アプリケーションの Spring Boot エントリーポイント。
 *
 * <p>スキー用品 EC サイト「SkiShop」の Spring Boot アプリケーションを起動するメインクラス。
 * {@code @SpringBootApplication} により、以下の機能が自動的に有効化される:</p>
 * <ul>
 *   <li><strong>コンポーネントスキャン</strong>: {@code com.skishop} パッケージ配下の
 *       全コンポーネント（Controller, Service, Repository 等）を自動検出・登録</li>
 *   <li><strong>自動構成</strong>: クラスパス上の依存関係に基づき、Spring MVC、
 *       Spring Data JPA、Spring Security、Thymeleaf 等を自動構成</li>
 *   <li><strong>プロパティ読み込み</strong>: {@code application.properties} および
 *       アクティブプロファイルに対応するプロパティファイルを読み込み</li>
 * </ul>
 *
 * <p>{@code @EnableScheduling} により、スケジュールタスク（例: メールキューの定期送信、
 * 期限切れカートのクリーンアップ等）のスケジューリングが有効化される。</p>
 *
 * <p>Struts 1.x からの移行コンテキスト: 旧システムでは {@code web.xml} による
 * Servlet/Filter 構成と Struts {@code ActionServlet} による起動を行っていたが、
 * Spring Boot の組み込み Tomcat + JAR パッケージング方式に移行した。</p>
 */
@SpringBootApplication
@EnableScheduling
public class SkiShopApplication {

    /**
     * アプリケーションのエントリーポイント。
     *
     * <p>Spring Boot の {@link SpringApplication#run} を呼び出し、
     * 組み込み Tomcat サーバーを起動してアプリケーションコンテキストを初期化する。</p>
     *
     * @param args コマンドライン引数（Spring Boot のプロパティオーバーライド等に使用可能）
     */
    public static void main(String[] args) {
        SpringApplication.run(SkiShopApplication.class, args);
    }
}
