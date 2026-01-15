package com.skishop.dao.mail;

import com.skishop.domain.mail.EmailQueue;
import java.util.List;

public interface EmailQueueDao {
  void enqueue(EmailQueue mail);

  void updateStatus(String id, String status, int retryCount, String lastError);

  List findByStatus(String status);
}
