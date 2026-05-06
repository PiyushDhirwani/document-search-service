package com.distributed_document.document_search_service.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDateTime;

@org.springframework.data.elasticsearch.annotations.Document(indexName = "documents", createIndex = false)
@Setting(shards = 3, replicas = 1)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentIndex {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String tenantId;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String content;

    @Field(type = FieldType.Keyword)
    private String author;

    @Field(type = FieldType.Text)
    private String metadata;

    @Field(type = FieldType.Keyword)
    private String[] tags;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime createdAt;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime updatedAt;

    public static DocumentIndex fromEntity(Document doc) {
        return DocumentIndex.builder()
                .id(doc.getId())
                .tenantId(doc.getTenantId())
                .title(doc.getTitle())
                .content(doc.getContent())
                .author(doc.getAuthor())
                .metadata(doc.getMetadata())
                .tags(doc.getTags() != null ? doc.getTags().split(",") : new String[]{})
                .status(doc.getStatus().name())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}
