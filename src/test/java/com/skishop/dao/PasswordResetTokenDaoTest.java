package com.skishop.dao;

import com.skishop.dao.user.PasswordResetTokenDao;
import com.skishop.dao.user.PasswordResetTokenDaoImpl;
import com.skishop.domain.user.PasswordResetToken;
import java.util.Date;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class PasswordResetTokenDaoTest extends DaoTestBase {
  private PasswordResetTokenDao tokenDao;

  @Before
  public void setUp() throws Exception {
    resetDatabase();
    tokenDao = new PasswordResetTokenDaoImpl();
  }

  @Test
  public void testFindAndMarkUsed() {
    PasswordResetToken token = tokenDao.findByToken("token-1");
    Assert.assertNotNull(token);

    tokenDao.markUsed(token.getId());
    PasswordResetToken updated = tokenDao.findByToken("token-1");
    Assert.assertNotNull(updated.getUsedAt());
  }

  @Test
  public void testInsert() {
    PasswordResetToken token = new PasswordResetToken();
    token.setId("prt-2");
    token.setUserId("u-1");
    token.setToken("token-2");
    token.setExpiresAt(new Date());
    tokenDao.insert(token);

    PasswordResetToken loaded = tokenDao.findByToken("token-2");
    Assert.assertNotNull(loaded);
  }
}
