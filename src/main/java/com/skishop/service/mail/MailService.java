package com.skishop.service.mail;

import com.skishop.dao.mail.EmailQueueDao;
import com.skishop.dao.mail.EmailQueueDaoImpl;
import com.skishop.domain.mail.EmailQueue;
import com.skishop.domain.order.Order;
import com.skishop.common.config.AppConfig;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class MailService {
  private static final int MAX_RETRY = 3;
  private static final long RETRY_DELAY_MS = 60000L;
  private static final long QUEUE_INTERVAL_MS = 30000L;
  private static final String TEMPLATE_PASSWORD_RESET = "mail/password_reset.txt";
  private static final String TEMPLATE_ORDER_CONFIRMATION = "mail/order_confirmation.txt";
  private static final String UTF_8 = "UTF-8";
  private static final Object TIMER_LOCK = new Object();
  private static Timer queueTimer;
  private static volatile boolean queueStarted;
  private static final MailConfig MAIL_CONFIG = loadConfig();
  private static final String PASSWORD_RESET_TEMPLATE = loadTemplate(
      TEMPLATE_PASSWORD_RESET,
      "Password reset token: {{token}}\n");
  private static final String ORDER_CONFIRMATION_TEMPLATE = loadTemplate(
      TEMPLATE_ORDER_CONFIRMATION,
      "Thank you for your order.\nOrder: {{orderNumber}}\nTotal: {{totalAmount}}\n");

  private final EmailQueueDao emailQueueDao = new EmailQueueDaoImpl();

  public MailService() {
    // Start queue processing once per JVM instance.
    startQueueProcessor();
  }

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

  public void enqueuePasswordReset(String to, String token) {
    String body = replace(PASSWORD_RESET_TEMPLATE, "{{token}}", token);
    enqueue(to, "Password reset", body);
  }

  public void enqueueOrderConfirmation(String to, Order order) {
    String body = ORDER_CONFIRMATION_TEMPLATE;
    body = replace(body, "{{orderNumber}}", order != null ? order.getOrderNumber() : "");
    body = replace(body, "{{totalAmount}}", order != null && order.getTotalAmount() != null ? order.getTotalAmount().toString() : "");
    enqueue(to, "Order confirmation", body);
  }

  public void processQueue() {
    List<EmailQueue> pending = emailQueueDao.findByStatus("PENDING");
    final Date now = new Date();
    for (EmailQueue mail : pending) {
      if (mail.getScheduledAt() != null && mail.getScheduledAt().after(now)) {
        continue;
      }
      handleSend(mail, now);
    }
  }

  public void shutdownQueue() {
    synchronized (TIMER_LOCK) {
      if (queueTimer != null) {
        queueTimer.cancel();
        queueTimer = null;
        queueStarted = false;
      }
    }
  }

  private void handleSend(EmailQueue mail, Date now) {
    try {
      send(mail);
      emailQueueDao.updateStatus(mail.getId(), "SENT", mail.getRetryCount(), null, mail.getScheduledAt(), new Date());
    } catch (MessagingException e) {
      int retryCount = mail.getRetryCount() + 1;
      String status = retryCount >= MAX_RETRY ? "FAILED" : "PENDING";
      Date nextSchedule = null;
      if ("PENDING".equals(status)) {
        nextSchedule = new Date(now.getTime() + RETRY_DELAY_MS);
      }
      emailQueueDao.updateStatus(mail.getId(), status, retryCount, formatError(e), nextSchedule, null);
    }
  }

  private void send(EmailQueue mail) throws MessagingException {
    Session session = createSession();
    Message message = new MimeMessage(session);
    String fromAddress = sanitizeAddress(MAIL_CONFIG.getMailFrom());
    String toAddress = sanitizeAddress(mail.getToAddr());
    message.setFrom(new InternetAddress(fromAddress));
    message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toAddress));
    message.setSubject(mail.getSubject());
    message.setSentDate(new Date());
    message.setText(mail.getBody());
    Transport.send(message);
  }

  private Session createSession() {
    Properties props = new Properties();
    props.put("mail.smtp.host", MAIL_CONFIG.getSmtpHost());
    props.put("mail.smtp.port", MAIL_CONFIG.getSmtpPort());
    if (MAIL_CONFIG.getSmtpUsername() != null) {
      props.put("mail.smtp.auth", "true");
      return Session.getInstance(props, new Authenticator() {
        protected PasswordAuthentication getPasswordAuthentication() {
          return new PasswordAuthentication(MAIL_CONFIG.getSmtpUsername(), MAIL_CONFIG.getSmtpPassword());
        }
      });
    }
    return Session.getInstance(props);
  }

  private void startQueueProcessor() {
    synchronized (TIMER_LOCK) {
      if (queueStarted) {
        return;
      }
      queueTimer = new Timer("mail-queue", true);
      queueStarted = true;
      queueTimer.schedule(new TimerTask() {
        public void run() {
          try {
            processQueue();
          } catch (RuntimeException e) {
            // Ignore errors to keep the queue timer running.
          }
        }
      }, QUEUE_INTERVAL_MS, QUEUE_INTERVAL_MS);
    }
  }

  private static MailConfig loadConfig() {
    AppConfig configSource = AppConfig.getInstance();
    MailConfig config = new MailConfig();
    config.setSmtpHost(normalize(configSource.getString("smtp.host")));
    config.setSmtpPort(normalize(configSource.getString("smtp.port")));
    config.setSmtpUsername(normalize(configSource.getString("smtp.username")));
    config.setSmtpPassword(normalize(configSource.getString("smtp.password")));
    config.setMailFrom(normalize(configSource.getString("mail.from")));
    if (config.getSmtpHost() == null) {
      throw new IllegalStateException("smtp.host is required");
    }
    if (config.getSmtpPort() == null) {
      config.setSmtpPort("25");
    }
    if (config.getMailFrom() == null) {
      config.setMailFrom("no-reply@localhost");
    }
    return config;
  }

  private static String loadTemplate(String path, String fallback) {
    InputStream input = MailService.class.getClassLoader().getResourceAsStream(path);
    if (input == null) {
      return fallback;
    }
    try {
      return readText(input);
    } catch (IOException e) {
      return fallback;
    }
  }

  private static String readText(InputStream input) throws IOException {
    java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(input, UTF_8));
    StringBuilder buffer = new StringBuilder();
    try {
      String line;
      while ((line = reader.readLine()) != null) {
        buffer.append(line);
        buffer.append("\n");
      }
    } finally {
      try {
        reader.close();
      } catch (IOException e) {
        // Ignore close failures.
      }
    }
    return buffer.toString();
  }

  private static String replace(String template, String token, String value) {
    if (template == null) {
      return value;
    }
    if (value == null) {
      value = "";
    }
    return template.replace(token, value);
  }

  private static String formatError(Exception e) {
    String message = e.getMessage();
    String detail = e.getClass().getName();
    if (message != null && message.length() > 0) {
      detail = detail + ": " + message;
    }
    if (detail.length() > 500) {
      detail = detail.substring(0, 500);
    }
    return detail;
  }

  private static String normalize(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    if (trimmed.length() == 0) {
      return null;
    }
    return trimmed;
  }

  private String sanitizeAddress(String address) throws MessagingException {
    if (address == null) {
      throw new MessagingException("Email address is required");
    }
    String trimmed = address.trim();
    if (trimmed.indexOf('\n') >= 0 || trimmed.indexOf('\r') >= 0) {
      throw new MessagingException("Invalid email address");
    }
    return trimmed;
  }

  // Holds SMTP configuration loaded from app.properties.
  private static final class MailConfig {
    private String smtpHost;
    private String smtpPort;
    private String smtpUsername;
    private String smtpPassword;
    private String mailFrom;

    private String getSmtpHost() {
      return smtpHost;
    }

    private void setSmtpHost(String smtpHost) {
      this.smtpHost = smtpHost;
    }

    private String getSmtpPort() {
      return smtpPort;
    }

    private void setSmtpPort(String smtpPort) {
      this.smtpPort = smtpPort;
    }

    private String getSmtpUsername() {
      return smtpUsername;
    }

    private void setSmtpUsername(String smtpUsername) {
      this.smtpUsername = smtpUsername;
    }

    private String getSmtpPassword() {
      return smtpPassword;
    }

    private void setSmtpPassword(String smtpPassword) {
      this.smtpPassword = smtpPassword;
    }

    private String getMailFrom() {
      return mailFrom;
    }

    private void setMailFrom(String mailFrom) {
      this.mailFrom = mailFrom;
    }
  }
}
