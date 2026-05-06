package com.distributed_document.document_search_service.dto;

import lombok.*;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResponse implements Serializable {

    private List<DocumentResponse> results;
    private long totalResults;
    private int page;
    private int size;
    private long queryTimeMs;
    private String query;
}
