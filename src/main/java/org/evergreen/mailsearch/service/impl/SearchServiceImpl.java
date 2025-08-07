package org.evergreen.mailsearch.service.impl;


import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import org.evergreen.mailsearch.model.EmailDocument;
import org.evergreen.mailsearch.service.SearchService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    public SearchServiceImpl(ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }

    @Override
    @Cacheable(value = "email_search", key = "#queryString + '_' + #dateRange + '_' + #category")
    public Map<String, Object> search(String queryString, String dateRange, String category) {
        System.out.printf("--- Executing search (Cache MISS) for query: %s, date: %s, category: %s ---%n", queryString, dateRange, category);

        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        if (!queryString.isBlank()) {
            boolQueryBuilder.must(m -> m
                    .multiMatch(mm -> mm
                            .query(queryString)
                            .fields("subject", "content", "fromAddress")
                    )
            );
        } else {
            boolQueryBuilder.must(m -> m.matchAll(ma -> ma));
        }

        if (!"all".equals(dateRange)) {
            LocalDate now = LocalDate.now();
            LocalDate startDate = null;
            if ("week".equals(dateRange)) {
                startDate = now.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
            } else if ("month".equals(dateRange)) {
                startDate = now.with(TemporalAdjusters.firstDayOfMonth());
            }

            if (startDate != null) {
                final LocalDate finalStartDate = startDate;
                boolQueryBuilder.filter(f -> f
                        .range(r -> r
                                .field("sentDate")
                                .gte(g -> g.stringValue(finalStartDate.toString()))
                        )
                );
            }
        }

        if (!"all".equals(category)) {
            boolQueryBuilder.filter(f -> f
                    .term(t -> t
                            .field("category")
                            .value(category)
                    )
            );
        }

        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(q -> q.bool(boolQueryBuilder.build()))
                .withPageable(PageRequest.of(0, 50))
                .build();

        SearchHits<EmailDocument> searchHits = elasticsearchOperations.search(nativeQuery, EmailDocument.class);
        List<EmailDocument> emails = searchHits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        Map<String, Object> results = new HashMap<>();
        results.put("emails", emails);
        results.put("totalHits", searchHits.getTotalHits());
        return results;
    }
}
