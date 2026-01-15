package com.skishop.dao;

import com.skishop.dao.point.PointTransactionDao;
import com.skishop.dao.point.PointTransactionDaoImpl;
import com.skishop.domain.point.PointTransaction;
import java.util.Date;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class PointTransactionDaoTest extends DaoTestBase {
  private PointTransactionDao pointTransactionDao;

  @Before
  public void setUp() throws Exception {
    resetDatabase();
    pointTransactionDao = new PointTransactionDaoImpl();
  }

  @Test
  public void testInsertAndList() {
    List initial = pointTransactionDao.listByUserId("u-1");
    Assert.assertFalse(initial.isEmpty());

    PointTransaction transaction = new PointTransaction();
    transaction.setId("pt-2");
    transaction.setUserId("u-1");
    transaction.setType("REDEEM");
    transaction.setAmount(-10);
    transaction.setReferenceId("order-1");
    transaction.setDescription("Use points");
    transaction.setExpiresAt(new Date());
    transaction.setExpired(false);
    transaction.setCreatedAt(new Date());
    pointTransactionDao.insert(transaction);

    List transactions = pointTransactionDao.listByUserId("u-1");
    Assert.assertTrue(transactions.size() >= 2);
  }
}
