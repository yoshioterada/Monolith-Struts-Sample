package com.skishop.web.action;

import com.skishop.domain.product.Product;
import com.skishop.service.catalog.ProductService;
import com.skishop.service.catalog.CategoryService;
import com.skishop.domain.product.Category;
import com.skishop.web.form.ProductSearchForm;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.util.LabelValueBean;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

public class ProductListAction extends Action {
  private static final int DEFAULT_SIZE = 10;
  private final ProductService productService = new ProductService();
  private final CategoryService categoryService = new CategoryService();

  public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    ProductSearchForm searchForm = (ProductSearchForm) form;
    int page = searchForm != null ? searchForm.getPage() : 1;
    int size = searchForm != null ? searchForm.getSize() : DEFAULT_SIZE;
    if (page <= 0) {
      page = 1;
    }
    if (size <= 0) {
      size = DEFAULT_SIZE;
    }
    String keyword = searchForm != null ? searchForm.getKeyword() : null;
    String categoryId = searchForm != null ? searchForm.getCategoryId() : null;
    String sort = searchForm != null ? searchForm.getSort() : null;
    int offset = (page - 1) * size;
    List<Product> products = productService.search(keyword, categoryId, sort, offset, size);
    List<Category> categories = categoryService.listAll();
    List<LabelValueBean> categoryOptions = new java.util.ArrayList<LabelValueBean>();
    categoryOptions.add(new LabelValueBean("指定なし", ""));
    if (categories != null) {
      for (Category c : categories) {
        categoryOptions.add(new LabelValueBean(c.getName(), c.getId()));
      }
    }
    request.setAttribute("productList", products);
    request.setAttribute("categoryOptions", categoryOptions);
    request.setAttribute("page", Integer.valueOf(page));
    request.setAttribute("size", Integer.valueOf(size));
    return mapping.getInputForward();
  }
}
