package com.skishop.dao;

import com.skishop.dao.inventory.InventoryDao;
import com.skishop.dao.inventory.InventoryDaoImpl;
import com.skishop.domain.inventory.Inventory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class InventoryDaoTest extends DaoTestBase {
  private InventoryDao inventoryDao;

  @Before
  public void setUp() throws Exception {
    resetDatabase();
    inventoryDao = new InventoryDaoImpl();
  }

  @Test
  public void testReserve() {
    boolean reserved = inventoryDao.reserve("P001", 2);
    Assert.assertTrue(reserved);
    Inventory inventory = inventoryDao.findByProductId("P001");
    Assert.assertEquals(2, inventory.getReservedQuantity());
  }
}
