package com.skishop.dao;

import com.skishop.dao.point.PointAccountDao;
import com.skishop.dao.point.PointAccountDaoImpl;
import com.skishop.domain.point.PointAccount;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class PointAccountDaoTest extends DaoTestBase {
  private PointAccountDao pointAccountDao;

  @Before
  public void setUp() throws Exception {
    resetDatabase();
    pointAccountDao = new PointAccountDaoImpl();
  }

  @Test
  public void testFindAndIncrement() {
    PointAccount account = pointAccountDao.findByUserId("u-1");
    Assert.assertNotNull(account);
    Assert.assertEquals(100, account.getBalance());

    pointAccountDao.increment("u-1", 50);
    PointAccount updated = pointAccountDao.findByUserId("u-1");
    Assert.assertEquals(150, updated.getBalance());
  }
}
