package com.distributed_document.document_search_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.distributed_document.document_search_service.repository")
public class DocumentSearchServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(DocumentSearchServiceApplication.class, args);
	}

}
