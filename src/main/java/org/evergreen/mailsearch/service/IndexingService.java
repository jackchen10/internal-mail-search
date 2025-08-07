package org.evergreen.mailsearch.service;

public interface IndexingService {
    void syncEmailsFromDbToEs();
}