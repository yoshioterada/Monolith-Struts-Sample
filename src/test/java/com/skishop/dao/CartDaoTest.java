package com.skishop.dao;

import com.skishop.dao.cart.CartDao;
import com.skishop.dao.cart.CartDaoImpl;
import com.skishop.domain.cart.Cart;
import com.skishop.domain.cart.CartItem;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CartDaoTest extends DaoTestBase {
  private CartDao cartDao;

  @Before
  public void setUp() throws Exception {
    resetDatabase();
    cartDao = new CartDaoImpl();
  }

  @Test
  public void testFindCartAndItems() {
    Cart cart = cartDao.findById("cart-1");
    Assert.assertNotNull(cart);
    List items = cartDao.findItemsByCartId("cart-1");
    Assert.assertEquals(1, items.size());
  }

  @Test
  public void testInsertAndUpdateItem() {
    Cart cart = new Cart();
    cart.setId("cart-2");
    cart.setUserId("u-1");
    cart.setSessionId("session-2");
    cart.setStatus("ACTIVE");
    cart.setExpiresAt(new Date());
    cartDao.insert(cart);

    CartItem item = new CartItem();
    item.setId("cart-item-2");
    item.setCartId("cart-2");
    item.setProductId("P001");
    item.setQuantity(1);
    item.setUnitPrice(new BigDecimal("50000"));
    cartDao.addItem(item);

    cartDao.updateItemQuantity("cart-item-2", 3);
    List items = cartDao.findItemsByCartId("cart-2");
    CartItem updated = (CartItem) items.get(0);
    Assert.assertEquals(3, updated.getQuantity());
  }
}
