package com.knowledge.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("个人知识管理系统 API")
                        .description("传统后端 + Redis/搜索 + RAG/Agent 的个人知识管理平台")
                        .version("v1.0.0")
                        .contact(new Contact().name("developer")));
    }
}
