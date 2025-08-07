package org.evergreen.mailsearch.repository;

import com.yourcompany.mailsearch.model.EmailDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailRepository extends ElasticsearchRepository<EmailDocument, String> {
}
