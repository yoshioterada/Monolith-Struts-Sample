package com.skishop.dao;

import com.skishop.dao.mail.EmailQueueDao;
import com.skishop.dao.mail.EmailQueueDaoImpl;
import com.skishop.domain.mail.EmailQueue;
import java.util.Date;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class EmailQueueDaoTest extends DaoTestBase {
  private EmailQueueDao emailQueueDao;

  @Before
  public void setUp() throws Exception {
    resetDatabase();
    emailQueueDao = new EmailQueueDaoImpl();
  }

  @Test
  public void testFindByStatusAndUpdate() {
    List pending = emailQueueDao.findByStatus("PENDING");
    Assert.assertEquals(1, pending.size());

    emailQueueDao.updateStatus("mail-1", "SENT", 1, null);
    List updated = emailQueueDao.findByStatus("SENT");
    Assert.assertEquals(1, updated.size());
  }

  @Test
  public void testEnqueue() {
    EmailQueue mail = new EmailQueue();
    mail.setId("mail-2");
    mail.setToAddr("admin@example.com");
    mail.setSubject("Alert");
    mail.setBody("Body");
    mail.setStatus("PENDING");
    mail.setRetryCount(0);
    mail.setLastError(null);
    mail.setScheduledAt(new Date());
    mail.setSentAt(null);
    emailQueueDao.enqueue(mail);

    List pending = emailQueueDao.findByStatus("PENDING");
    Assert.assertTrue(pending.size() >= 2);
  }
}
