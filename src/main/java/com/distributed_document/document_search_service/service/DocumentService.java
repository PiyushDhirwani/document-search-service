package com.distributed_document.document_search_service.service;

import com.distributed_document.document_search_service.dto.*;
import com.distributed_document.document_search_service.exception.DocumentNotFoundException;
import com.distributed_document.document_search_service.model.Document;
import com.distributed_document.document_search_service.model.DocumentIndex;
import com.distributed_document.document_search_service.repository.DocumentRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final ElasticsearchService elasticsearchService;

    @Autowired
    public DocumentService(DocumentRepository documentRepository,
                           @Autowired(required = false) ElasticsearchService elasticsearchService) {
        this.documentRepository = documentRepository;
        this.elasticsearchService = elasticsearchService;
        if (elasticsearchService == null) {
            log.info("Elasticsearch is disabled. Using MySQL full-text search.");
        }
    }

    @Transactional
    @CacheEvict(value = "searches", allEntries = true)
    public DocumentResponse createDocument(String tenantId, DocumentRequest request) {
        log.info("Creating document for tenant: {}", tenantId);

        Document document = Document.builder()
                .tenantId(tenantId)
                .title(request.getTitle())
                .content(request.getContent())
                .author(request.getAuthor() != null ? request.getAuthor() : "unknown")
                .metadata(request.getMetadata())
                .tags(request.getTags() != null ? String.join(",", request.getTags()) : null)
                .status(Document.DocumentStatus.ACTIVE)
                .build();

        Document saved = documentRepository.save(document);
        log.info("Document created with id: {} for tenant: {}", saved.getId(), tenantId);

        // Index in Elasticsearch (async-safe, won't fail the request)
        if (elasticsearchService != null) {
            elasticsearchService.indexDocument(saved);
        }

        return DocumentResponse.fromEntity(saved);
    }

    @Cacheable(value = "documents", key = "#tenantId + ':' + #documentId")
    @CircuitBreaker(name = "documentService", fallbackMethod = "getDocumentFallback")
    public DocumentResponse getDocument(String tenantId, String documentId) {
        log.info("Fetching document: {} for tenant: {}", documentId, tenantId);

        Document document = documentRepository.findByIdAndTenantId(documentId, tenantId)
                .orElseThrow(() -> new DocumentNotFoundException(
                        "Document not found: " + documentId));

        return DocumentResponse.fromEntity(document);
    }

    @Transactional
    @CacheEvict(value = {"documents", "searches"}, allEntries = true)
    public void deleteDocument(String tenantId, String documentId) {
        log.info("Deleting document: {} for tenant: {}", documentId, tenantId);

        Document document = documentRepository.findByIdAndTenantId(documentId, tenantId)
                .orElseThrow(() -> new DocumentNotFoundException(
                        "Document not found: " + documentId));

        document.setStatus(Document.DocumentStatus.DELETED);
        documentRepository.save(document);

        // Remove from Elasticsearch index
        if (elasticsearchService != null) {
            elasticsearchService.deleteDocument(documentId);
        }

        log.info("Document soft-deleted: {}", documentId);
    }

    @CircuitBreaker(name = "documentService", fallbackMethod = "searchDocumentsFallback")
    public SearchResponse searchDocuments(String tenantId, String query, int page, int size) {
        log.info("Searching documents for tenant: {}, query: '{}', page: {}, size: {}",
                tenantId, query, page, size);

        long startTime = System.currentTimeMillis();
        Pageable pageable = PageRequest.of(page, size);

        List<DocumentResponse> documentResponses;
        long totalResults;

        // Try Elasticsearch first, fall back to MySQL
        try {
            if (elasticsearchService == null) throw new RuntimeException("ES disabled");
            Page<DocumentIndex> esResults = elasticsearchService.search(tenantId, query, pageable);
            documentResponses = esResults.getContent().stream()
                    .map(this::toResponse)
                    .toList();
            totalResults = esResults.getTotalElements();
            log.info("Search served from Elasticsearch");
        } catch (Exception e) {
            log.info("Using MySQL search (ES unavailable): {}", e.getMessage());
            Page<Document> results;
            try {
                results = documentRepository.fullTextSearch(tenantId, query, pageable);
            } catch (Exception ex) {
                log.warn("MySQL full-text search failed, falling back to LIKE: {}", ex.getMessage());
                results = documentRepository.searchByQuery(tenantId, query, pageable);
            }
            documentResponses = results.getContent().stream()
                    .map(DocumentResponse::fromEntity)
                    .toList();
            totalResults = results.getTotalElements();
        }

        long queryTime = System.currentTimeMillis() - startTime;

        SearchResponse response = SearchResponse.builder()
                .results(documentResponses)
                .totalResults(totalResults)
                .page(page)
                .size(size)
                .queryTimeMs(queryTime)
                .query(query)
                .build();

        log.info("Search completed in {}ms, found {} results", queryTime, totalResults);
        return response;
    }

    private DocumentResponse toResponse(DocumentIndex index) {
        return DocumentResponse.builder()
                .id(index.getId())
                .tenantId(index.getTenantId())
                .title(index.getTitle())
                .content(index.getContent())
                .author(index.getAuthor())
                .metadata(index.getMetadata())
                .tags(index.getTags() != null ? List.of(index.getTags()) : List.of())
                .status(index.getStatus())
                .createdAt(index.getCreatedAt())
                .updatedAt(index.getUpdatedAt())
                .build();
    }

    // Circuit breaker fallback methods
    public DocumentResponse getDocumentFallback(String tenantId, String documentId, Throwable t) {
        log.error("Circuit breaker triggered for getDocument: {}", t.getMessage());
        throw new RuntimeException("Service temporarily unavailable. Please try again later.");
    }

    public SearchResponse searchDocumentsFallback(String tenantId, String query, int page, int size, Throwable t) {
        log.error("Circuit breaker triggered for search: {}", t.getMessage());
        return SearchResponse.builder()
                .results(List.of())
                .totalResults(0)
                .page(page)
                .size(size)
                .queryTimeMs(-1)
                .query(query)
                .build();
    }
}
