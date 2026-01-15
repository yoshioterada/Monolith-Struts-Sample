package com.skishop.dao.inventory;

import com.skishop.domain.inventory.Inventory;

public interface InventoryDao {
  Inventory findByProductId(String productId);

  boolean reserve(String productId, int quantity);
}
