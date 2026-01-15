package com.skishop.dao;

import com.skishop.dao.shipping.ShippingMethodDao;
import com.skishop.dao.shipping.ShippingMethodDaoImpl;
import com.skishop.domain.shipping.ShippingMethod;
import java.math.BigDecimal;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ShippingMethodDaoTest extends DaoTestBase {
  private ShippingMethodDao shippingMethodDao;

  @Before
  public void setUp() throws Exception {
    resetDatabase();
    shippingMethodDao = new ShippingMethodDaoImpl();
  }

  @Test
  public void testListActiveAndFind() {
    List methods = shippingMethodDao.listActive();
    Assert.assertFalse(methods.isEmpty());

    ShippingMethod method = shippingMethodDao.findByCode("STANDARD");
    Assert.assertNotNull(method);
  }

  @Test
  public void testInsert() {
    ShippingMethod method = new ShippingMethod();
    method.setId("ship-exp");
    method.setCode("EXPRESS");
    method.setName("Express");
    method.setFee(new BigDecimal("1200"));
    method.setActive(true);
    method.setSortOrder(2);
    shippingMethodDao.insert(method);

    ShippingMethod loaded = shippingMethodDao.findByCode("EXPRESS");
    Assert.assertNotNull(loaded);
  }
}
