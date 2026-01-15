package com.skishop.dao.cart;

import com.skishop.domain.cart.Cart;
import com.skishop.domain.cart.CartItem;
import java.util.List;

public interface CartDao {
  Cart findById(String id);

  void insert(Cart cart);

  void addItem(CartItem item);

  List<CartItem> findItemsByCartId(String cartId);

  void updateItemQuantity(String itemId, int quantity);

  void deleteItemsByCartId(String cartId);

  void updateStatus(String cartId, String status);
}
