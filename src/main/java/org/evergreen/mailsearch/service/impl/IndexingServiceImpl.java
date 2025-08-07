package org.evergreen.mailsearch.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.evergreen.mailsearch.entity.Email;
import org.evergreen.mailsearch.mapper.EmailMapper;
import org.evergreen.mailsearch.model.EmailDocument;
import org.evergreen.mailsearch.repository.EmailRepository;
import org.evergreen.mailsearch.service.IndexingService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class IndexingServiceImpl implements IndexingService {

    private final EmailMapper emailMapper;
    private final EmailRepository emailRepository;

    public IndexingServiceImpl(EmailMapper emailMapper, EmailRepository emailRepository) {
        this.emailMapper = emailMapper;
        this.emailRepository = emailRepository;
    }

    @Override
    @Scheduled(initialDelay = 20000, fixedDelay = 600000) // 启动20秒后执行，之后每10分钟同步一次
    public void syncEmailsFromDbToEs() {
        System.out.println(">>> [SCHEDULER] Starting DB to Elasticsearch sync task...");
        try {
            // 查询过去15分钟内有更新的邮件记录
            LocalDateTime fifteenMinutesAgo = LocalDateTime.now().minusMinutes(15);
            QueryWrapper<Email> queryWrapper = new QueryWrapper<>();
            queryWrapper.ge("update_time", fifteenMinutesAgo);
            List<Email> emailsToUpdate = emailMapper.selectList(queryWrapper);

            if (emailsToUpdate.isEmpty()) {
                System.out.println(">>> [SCHEDULER] No new emails to sync.");
                return;
            }

            List<EmailDocument> emailDocuments = emailsToUpdate.stream()
                    .map(this::convertToDocument)
                    .collect(Collectors.toList());

            emailRepository.saveAll(emailDocuments);
            System.out.printf(">>> [SCHEDULER] Synced %d emails to Elasticsearch.%n", emailDocuments.size());

        } catch (Exception e) {
            System.err.println(">>> [SCHEDULER] Error during DB to ES sync task: " + e.getMessage());
        }
    }

    private EmailDocument convertToDocument(Email email) {
        EmailDocument doc = new EmailDocument();
        doc.setId(String.valueOf(email.getId()));
        doc.setFromAddress(email.getFromAddress());
        doc.setSubject(email.getSubject());
        doc.setContent(email.getContent());
        doc.setSentDate(email.getSentDate());
        doc.setCategory(email.getCategory());
        return doc;
    }
}
