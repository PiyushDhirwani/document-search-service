package com.distributed_document.document_search_service.repository;

import com.distributed_document.document_search_service.model.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, String> {

    Optional<Document> findByIdAndTenantId(String id, String tenantId);

    Page<Document> findByTenantIdAndStatus(String tenantId, Document.DocumentStatus status, Pageable pageable);

    @Query(value = "SELECT * FROM documents d WHERE d.tenant_id = :tenantId " +
            "AND d.status = 'ACTIVE' " +
            "AND (LOWER(d.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(d.content) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(d.tags) LIKE LOWER(CONCAT('%', :query, '%')))",
            countQuery = "SELECT COUNT(*) FROM documents d WHERE d.tenant_id = :tenantId " +
                    "AND d.status = 'ACTIVE' " +
                    "AND (LOWER(d.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
                    "OR LOWER(d.content) LIKE LOWER(CONCAT('%', :query, '%')) " +
                    "OR LOWER(d.tags) LIKE LOWER(CONCAT('%', :query, '%')))",
            nativeQuery = true)
    Page<Document> searchByQuery(@Param("tenantId") String tenantId,
                                 @Param("query") String query,
                                 Pageable pageable);

    @Query(value = "SELECT *, " +
            "MATCH(title, content) AGAINST(:query IN NATURAL LANGUAGE MODE) AS relevance " +
            "FROM documents d WHERE d.tenant_id = :tenantId " +
            "AND d.status = 'ACTIVE' " +
            "AND MATCH(title, content) AGAINST(:query IN NATURAL LANGUAGE MODE) " +
            "ORDER BY relevance DESC",
            countQuery = "SELECT COUNT(*) FROM documents d WHERE d.tenant_id = :tenantId " +
                    "AND d.status = 'ACTIVE' " +
                    "AND MATCH(title, content) AGAINST(:query IN NATURAL LANGUAGE MODE)",
            nativeQuery = true)
    Page<Document> fullTextSearch(@Param("tenantId") String tenantId,
                                  @Param("query") String query,
                                  Pageable pageable);

    long countByTenantId(String tenantId);
}
