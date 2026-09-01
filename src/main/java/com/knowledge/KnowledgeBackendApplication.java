package com.knowledge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KnowledgeBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnowledgeBackendApplication.class, args);
    }
}
