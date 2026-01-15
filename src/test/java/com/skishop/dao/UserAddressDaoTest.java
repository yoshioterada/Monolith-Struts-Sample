package com.skishop.dao;

import com.skishop.dao.address.UserAddressDao;
import com.skishop.dao.address.UserAddressDaoImpl;
import com.skishop.domain.address.Address;
import java.util.Date;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class UserAddressDaoTest extends DaoTestBase {
  private UserAddressDao userAddressDao;

  @Before
  public void setUp() throws Exception {
    resetDatabase();
    userAddressDao = new UserAddressDaoImpl();
  }

  @Test
  public void testListAndSave() {
    List addresses = userAddressDao.listByUserId("u-1");
    Assert.assertEquals(1, addresses.size());

    Address address = new Address();
    address.setId("addr-2");
    address.setUserId("u-1");
    address.setLabel("会社");
    address.setRecipientName("山田 太郎");
    address.setPostalCode("150-0001");
    address.setPrefecture("東京都");
    address.setAddress1("渋谷区");
    address.setAddress2("ビル2F");
    address.setPhone("0311111111");
    address.setDefault(false);
    address.setCreatedAt(new Date());
    address.setUpdatedAt(new Date());
    userAddressDao.save(address);

    List updated = userAddressDao.listByUserId("u-1");
    Assert.assertEquals(2, updated.size());
  }
}
