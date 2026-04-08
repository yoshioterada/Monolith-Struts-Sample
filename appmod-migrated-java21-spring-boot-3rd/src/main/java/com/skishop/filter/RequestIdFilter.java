package com.skishop.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * HTTP リクエストごとに一意のリクエスト ID を生成・伝播するサーブレットフィルター。
 *
 * <p>分散トレーシングとログの相関付けを目的として、以下の処理を行う:</p>
 * <ol>
 *   <li>受信リクエストの {@code X-Request-Id} ヘッダーを確認する</li>
 *   <li>ヘッダーが存在する場合はその値を使用し、存在しない場合は UUID v4 で新規生成する</li>
 *   <li>リクエスト ID を SLF4J の {@link MDC}（Mapped Diagnostic Context）に
 *       キー {@code reqId} で格納する（ログパターンに {@code %X{reqId}} を含めることで
 *       全ログ出力にリクエスト ID を付与できる）</li>
 *   <li>レスポンスヘッダー {@code X-Request-Id} にリクエスト ID を設定する
 *       （クライアント側でのトレーシングに使用）</li>
 *   <li>リクエスト処理完了後、MDC からリクエスト ID を削除する（スレッド汚染防止）</li>
 * </ol>
 *
 * <p>{@link OncePerRequestFilter} を継承しているため、フォワードやインクルードが
 * 発生しても 1 リクエストにつき 1 回のみ実行される。</p>
 *
 * <p>Struts 1.x からの移行コンテキスト: 旧システムの
 * {@code com.skishop.web.filter.RequestIdFilter}（{@code web.xml} で登録）から
 * Spring の {@link OncePerRequestFilter} + {@code @Component} による自動登録に移行した。</p>
 *
 * @see org.slf4j.MDC ログの診断コンテキスト
 */
@Component
public class RequestIdFilter extends OncePerRequestFilter {

    /** リクエスト ID を格納する HTTP ヘッダー名。 */
    private static final String HEADER_NAME = "X-Request-Id";

    /** SLF4J MDC に格納する際のキー名。ログパターンで {@code %X{reqId}} として参照される。 */
    private static final String MDC_KEY = "reqId";

    private static final int MAX_REQUEST_ID_LENGTH = 64;

    private static final java.util.regex.Pattern VALID_REQUEST_ID =
            java.util.regex.Pattern.compile("^[a-zA-Z0-9\\-]{1,64}$");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String requestId = request.getHeader(HEADER_NAME);
        if (!StringUtils.hasText(requestId)
                || requestId.length() > MAX_REQUEST_ID_LENGTH
                || !VALID_REQUEST_ID.matcher(requestId).matches()) {
            requestId = UUID.randomUUID().toString();
        }
        MDC.put(MDC_KEY, requestId);
        response.setHeader(HEADER_NAME, requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
