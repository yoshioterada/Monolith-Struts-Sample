package com.skishop.exception;

/**
 * 指定されたリソースが見つからない場合にスローされる例外（HTTP 404 に対応）。
 *
 * <p>Repository 層でエンティティの検索結果が空の場合に、Service 層でスローされる。
 * リソースの種別名（{@code resourceName}）と識別子（{@code resourceId}）を保持し、
 * ログ出力とエラーメッセージの生成に使用する。</p>
 *
 * <p>使用例:</p>
 * <pre>{@code
 * Product product = productRepository.findById(id)
 *     .orElseThrow(() -> new ResourceNotFoundException("Product", id));
 * }</pre>
 *
 * <p>{@link GlobalExceptionHandler#handleNotFound(ResourceNotFoundException, org.springframework.ui.Model)}
 * で捕捉され、{@code error/404} テンプレートが返される。</p>
 *
 * @see GlobalExceptionHandler#handleNotFound(ResourceNotFoundException, org.springframework.ui.Model)
 */
public class ResourceNotFoundException extends RuntimeException {

    /** 見つからなかったリソースの種別名（例: "Product", "User", "Order"）。 */
    private final String resourceName;

    /** 検索に使用されたリソースの識別子。 */
    private final String resourceId;

    /**
     * リソース未検出例外を生成する。
     *
     * <p>例外メッセージは {@code "<resourceName> not found with id: <resourceId>"} 形式で自動生成される。</p>
     *
     * @param resourceName リソースの種別名（例: "Product", "User"）
     * @param resourceId   見つからなかったリソースの識別子
     */
    public ResourceNotFoundException(String resourceName, String resourceId) {
        super("%s not found with id: %s".formatted(resourceName, resourceId));
        this.resourceName = resourceName;
        this.resourceId = resourceId;
    }

    /**
     * リソースの種別名を返す。
     *
     * @return リソースの種別名（例: "Product", "User", "Order"）
     */
    public String getResourceName() {
        return resourceName;
    }

    /**
     * 見つからなかったリソースの識別子を返す。
     *
     * @return リソースの識別子
     */
    public String getResourceId() {
        return resourceId;
    }
}
