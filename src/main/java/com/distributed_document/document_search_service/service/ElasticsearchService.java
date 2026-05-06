package com.distributed_document.document_search_service.service;

import com.distributed_document.document_search_service.model.Document;
import com.distributed_document.document_search_service.model.DocumentIndex;
import com.distributed_document.document_search_service.repository.es.DocumentSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.elasticsearch.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchService {

    private final DocumentSearchRepository documentSearchRepository;

    public void indexDocument(Document document) {
        try {
            DocumentIndex index = DocumentIndex.fromEntity(document);
            documentSearchRepository.save(index);
            log.info("Document indexed in ES: {}", document.getId());
        } catch (Exception e) {
            log.error("Failed to index document in ES: {}, error: {}", document.getId(), e.getMessage());
        }
    }

    public void deleteDocument(String documentId) {
        try {
            documentSearchRepository.deleteById(documentId);
            log.info("Document removed from ES index: {}", documentId);
        } catch (Exception e) {
            log.error("Failed to delete document from ES: {}, error: {}", documentId, e.getMessage());
        }
    }

    public Page<DocumentIndex> search(String tenantId, String query, Pageable pageable) {
        return documentSearchRepository.searchByTenantAndQuery(tenantId, query, pageable);
    }

    public boolean isAvailable() {
        try {
            documentSearchRepository.count();
            return true;
        } catch (Exception e) {
            log.warn("Elasticsearch is not available: {}", e.getMessage());
            return false;
        }
    }
}
