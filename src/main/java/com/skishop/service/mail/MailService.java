package com.skishop.service.mail;

import com.skishop.dao.mail.EmailQueueDao;
import com.skishop.dao.mail.EmailQueueDaoImpl;
import com.skishop.domain.mail.EmailQueue;
import java.util.Date;
import java.util.UUID;

public class MailService {
  private final EmailQueueDao emailQueueDao = new EmailQueueDaoImpl();

  public void enqueue(String to, String subject, String body) {
    EmailQueue mail = new EmailQueue();
    mail.setId(UUID.randomUUID().toString());
    mail.setToAddr(to);
    mail.setSubject(subject);
    mail.setBody(body);
    mail.setStatus("PENDING");
    mail.setRetryCount(0);
    mail.setLastError(null);
    mail.setScheduledAt(new Date());
    mail.setSentAt(null);
    emailQueueDao.enqueue(mail);
  }
}
