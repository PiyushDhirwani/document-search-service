package com.distributed_document.document_search_service.controller;

import com.distributed_document.document_search_service.dto.*;
import com.distributed_document.document_search_service.service.DocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/documents")
    public ResponseEntity<DocumentResponse> createDocument(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @Valid @RequestBody DocumentRequest request) {
        DocumentResponse response = documentService.createDocument(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/documents/{id}")
    public ResponseEntity<DocumentResponse> getDocument(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String id) {
        DocumentResponse response = documentService.getDocument(tenantId, id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/documents/{id}")
    public ResponseEntity<Map<String, String>> deleteDocument(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String id) {
        documentService.deleteDocument(tenantId, id);
        return ResponseEntity.ok(Map.of("message", "Document deleted successfully", "id", id));
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> searchDocuments(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam("q") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        SearchResponse response = documentService.searchDocuments(tenantId, query, page, size);
        return ResponseEntity.ok(response);
    }
}
