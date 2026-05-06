package com.distributed_document.document_search_service.dto;

import com.distributed_document.document_search_service.model.Document;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentResponse implements Serializable {

    private String id;
    private String tenantId;
    private String title;
    private String content;
    private String author;
    private String metadata;
    private List<String> tags;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DocumentResponse fromEntity(Document doc) {
        return DocumentResponse.builder()
                .id(doc.getId())
                .tenantId(doc.getTenantId())
                .title(doc.getTitle())
                .content(doc.getContent())
                .author(doc.getAuthor())
                .metadata(doc.getMetadata())
                .tags(doc.getTags() != null ? Arrays.asList(doc.getTags().split(",")) : List.of())
                .status(doc.getStatus().name())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}
