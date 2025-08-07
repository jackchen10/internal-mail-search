package org.evergreen.mailsearch.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMultipart;
import org.evergreen.mailsearch.entity.Email;
import org.evergreen.mailsearch.mapper.EmailMapper;
import org.evergreen.mailsearch.service.MailFetchingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Properties;

@Service
public class MailFetchingServiceImpl implements MailFetchingService {

    private final EmailMapper emailMapper;

    @Value("${mail.store.protocol}")
    private String protocol;
    @Value("${mail.imap.host}")
    private String host;
    @Value("${mail.imap.port}")
    private int port;
    @Value("${mail.user}")
    private String user;
    @Value("${mail.password}")
    private String password;

    public MailFetchingServiceImpl(EmailMapper emailMapper) {
        this.emailMapper = emailMapper;
    }

    @Override
    @Scheduled(initialDelay = 5000, fixedRate = 3600000)
    public void fetchAndSaveEmails() {
        System.out.println(">>> [SCHEDULER] Starting email fetch task...");
        try {
            Properties props = new Properties();
            props.put("mail.store.protocol", protocol);
            props.put(String.format("mail.%s.host", protocol), host);
            props.put(String.format("mail.%s.port", protocol), port);
            props.put(String.format("mail.%s.ssl.enable", protocol), "true");

            Session session = Session.getInstance(props, null);
            Store store = session.getStore(protocol);
            store.connect(host, user, password);

            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            int newMailCount = 0;
            for (Message message : inbox.getMessages()) {
                String messageId = getMessageId(message);

                QueryWrapper<Email> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("message_id", messageId);
                if (emailMapper.exists(queryWrapper)) {
                    continue;
                }

                Email email = new Email();
                email.setMessageId(messageId);
                email.setFromAddress(Address.toString(message.getFrom()));
                email.setSubject(message.getSubject());
                email.setContent(getTextFromMessage(message));
                email.setSentDate(message.getSentDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
                email.setCategory(determineCategory(email.getSubject(), email.getContent()));
                email.setCreateTime(LocalDateTime.now());
                email.setUpdateTime(LocalDateTime.now());

                emailMapper.insert(email);
                newMailCount++;
            }

            System.out.printf(">>> [SCHEDULER] Email fetch task finished. Saved %d new emails to database.%n", newMailCount);
            inbox.close(false);
            store.close();
        } catch (Exception e) {
            System.err.println(">>> [SCHEDULER] Error during email fetch task: " + e.getMessage());
        }
    }

    private String getMessageId(Message message) throws MessagingException {
        String[] headers = message.getHeader("Message-ID");
        if (headers != null && headers.length > 0) {
            return headers[0];
        }
        // Fallback for messages without a Message-ID header
        return "fallback_" + user + "_" + message.getSentDate().getTime() + "_" + message.getSubject();
    }

    private String getTextFromMessage(Message message) throws MessagingException, IOException {
        if (message.isMimeType("text/plain")) {
            return message.getContent().toString();
        }
        if (message.isMimeType("multipart/*")) {
            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < mimeMultipart.getCount(); i++) {
                BodyPart bodyPart = mimeMultipart.getBodyPart(i);
                if (bodyPart.isMimeType("text/plain")) {
                    result.append(bodyPart.getContent().toString());
                    break;
                }
            }
            return result.toString();
        }
        return "";
    }

    private String determineCategory(String subject, String content) {
        if (subject == null) subject = "";
        String lowerCaseSubject = subject.toLowerCase();

        if (lowerCaseSubject.contains("会议") || lowerCaseSubject.contains("meeting") || lowerCaseSubject.contains("评审")) {
            return "MEETING_NOTICE";
        }
        if (lowerCaseSubject.contains("待办") || lowerCaseSubject.contains("todo") || lowerCaseSubject.contains("请处理")) {
            return "TODO_ITEM";
        }
        if (lowerCaseSubject.contains("通知") || lowerCaseSubject.contains("announcement")) {
            return "IMPORTANT_NOTICE";
        }
        if (lowerCaseSubject.contains("考试") || lowerCaseSubject.contains("填报") || lowerCaseSubject.contains("报名")) {
            return "EXAM_FORM_NOTICE";
        }
        return "OTHERS";
    }
}
