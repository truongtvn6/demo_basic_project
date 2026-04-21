package com.demobasic.integration.config;

import com.demobasic.integration.github.GithubProperties;
import com.demobasic.integration.jira.JiraProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties({JiraProperties.class, GithubProperties.class})
public class IntegrationConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
