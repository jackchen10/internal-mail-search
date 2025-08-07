package org.evergreen.mailsearch.service;

import java.util.Map;

public interface SearchService {
    Map<String, Object> search(String queryString, String dateRange, String category);
}