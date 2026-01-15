package com.skishop.service.cart;

import com.skishop.dao.cart.CartDao;
import com.skishop.dao.cart.CartDaoImpl;
import com.skishop.domain.cart.Cart;
import com.skishop.domain.cart.CartItem;
import java.math.BigDecimal;
import java.util.List;

public class CartService {
  private final CartDao cartDao = new CartDaoImpl();

  public Cart getCart(String cartId) {
    return cartDao.findById(cartId);
  }

  public List<CartItem> getItems(String cartId) {
    return cartDao.findItemsByCartId(cartId);
  }

  public BigDecimal calculateSubtotal(List<CartItem> items) {
    BigDecimal subtotal = BigDecimal.ZERO;
    if (items == null) {
      return subtotal;
    }
    for (CartItem item : items) {
      BigDecimal line = item.getUnitPrice().multiply(new BigDecimal(item.getQuantity()));
      subtotal = subtotal.add(line);
    }
    return subtotal;
  }

  public void clearCart(String cartId) {
    cartDao.deleteItemsByCartId(cartId);
    cartDao.updateStatus(cartId, "CHECKED_OUT");
  }
}
