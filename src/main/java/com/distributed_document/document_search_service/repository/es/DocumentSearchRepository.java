package com.distributed_document.document_search_service.repository.es;

import com.distributed_document.document_search_service.model.DocumentIndex;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentSearchRepository extends ElasticsearchRepository<DocumentIndex, String> {

    Page<DocumentIndex> findByTenantIdAndStatus(String tenantId, String status, Pageable pageable);

    @Query("""
        {
          "bool": {
            "must": [
              {
                "multi_match": {
                  "query": "?1",
                  "fields": ["title^3", "content", "tags^2", "metadata"],
                  "type": "best_fields",
                  "fuzziness": "AUTO"
                }
              }
            ],
            "filter": [
              { "term": { "tenantId": "?0" } },
              { "term": { "status": "ACTIVE" } }
            ]
          }
        }
    """)
    Page<DocumentIndex> searchByTenantAndQuery(String tenantId, String query, Pageable pageable);
}
