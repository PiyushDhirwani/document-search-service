package com.distributed_document.document_search_service.service;

import com.distributed_document.document_search_service.dto.*;
import com.distributed_document.document_search_service.exception.DocumentNotFoundException;
import com.distributed_document.document_search_service.model.Document;
import com.distributed_document.document_search_service.repository.DocumentRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;

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
        log.info("Document soft-deleted: {}", documentId);
    }

    @CircuitBreaker(name = "documentService", fallbackMethod = "searchDocumentsFallback")
    public SearchResponse searchDocuments(String tenantId, String query, int page, int size) {
        log.info("Searching documents for tenant: {}, query: '{}', page: {}, size: {}",
                tenantId, query, page, size);

        long startTime = System.currentTimeMillis();
        Pageable pageable = PageRequest.of(page, size);

        Page<Document> results;
        try {
            results = documentRepository.fullTextSearch(tenantId, query, pageable);
        } catch (Exception e) {
            log.warn("Full-text search failed, falling back to LIKE search: {}", e.getMessage());
            results = documentRepository.searchByQuery(tenantId, query, pageable);
        }

        long queryTime = System.currentTimeMillis() - startTime;

        List<DocumentResponse> documentResponses = results.getContent().stream()
                .map(DocumentResponse::fromEntity)
                .toList();

        SearchResponse response = SearchResponse.builder()
                .results(documentResponses)
                .totalResults(results.getTotalElements())
                .page(page)
                .size(size)
                .queryTimeMs(queryTime)
                .query(query)
                .build();

        log.info("Search completed in {}ms, found {} results", queryTime, results.getTotalElements());
        return response;
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
