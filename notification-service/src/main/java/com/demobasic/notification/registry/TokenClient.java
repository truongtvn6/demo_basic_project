package com.demobasic.notification.registry;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Component
public class TokenClient {

    private static final Logger log = LoggerFactory.getLogger(TokenClient.class);

    private final WebClient webClient;

    public TokenClient(@Value("${demobasic.registry.base-url}") String baseUrl) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    public List<String> fetchTokens() {
        try {
            JsonNode body = webClient.get()
                    .uri("/internal/tokens")
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            if (body == null || !body.isArray()) {
                return List.of();
            }
            List<String> tokens = new ArrayList<>();
            Iterator<JsonNode> it = body.elements();
            while (it.hasNext()) {
                String t = it.next().path("token").asText(null);
                if (t != null && !t.isBlank()) {
                    tokens.add(t);
                }
            }
            return tokens;
        } catch (Exception ex) {
            log.error("Failed to fetch tokens from registry: {}", ex.getMessage());
            return List.of();
        }
    }
}
