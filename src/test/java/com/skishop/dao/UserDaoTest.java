package com.skishop.dao;

import com.skishop.dao.user.UserDao;
import com.skishop.dao.user.UserDaoImpl;
import com.skishop.domain.user.User;
import java.util.Date;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class UserDaoTest extends DaoTestBase {
  private UserDao userDao;

  @Before
  public void setUp() throws Exception {
    resetDatabase();
    userDao = new UserDaoImpl();
  }

  @Test
  public void testFindByEmail() {
    User user = userDao.findByEmail("user@example.com");
    Assert.assertNotNull(user);
    Assert.assertEquals("u-1", user.getId());
  }

  @Test
  public void testInsertAndUpdateStatus() {
    User user = new User();
    user.setId("u-2");
    user.setEmail("new@example.com");
    user.setUsername("new");
    user.setPasswordHash("hash2");
    user.setSalt("salt2");
    user.setStatus("ACTIVE");
    user.setRole("USER");
    user.setCreatedAt(new Date());
    user.setUpdatedAt(new Date());
    userDao.insert(user);

    User loaded = userDao.findById("u-2");
    Assert.assertNotNull(loaded);
    Assert.assertEquals("new@example.com", loaded.getEmail());

    userDao.updateStatus("u-2", "LOCKED");
    User updated = userDao.findById("u-2");
    Assert.assertEquals("LOCKED", updated.getStatus());
  }
}
