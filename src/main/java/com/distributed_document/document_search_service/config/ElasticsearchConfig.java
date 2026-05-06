package com.distributed_document.document_search_service.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@ConditionalOnProperty(name = "app.elasticsearch.enabled", havingValue = "true")
@EnableElasticsearchRepositories(basePackages = "com.distributed_document.document_search_service.repository.es")
public class ElasticsearchConfig {
}
