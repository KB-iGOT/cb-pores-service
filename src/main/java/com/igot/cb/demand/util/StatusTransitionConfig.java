package com.igot.cb.demand.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Configuration
@AllArgsConstructor
public class StatusTransitionConfig {
    private Map<String, Map<String, Set<String>>> transitions = new HashMap<>();

    public StatusTransitionConfig() throws IOException {
        this("/payloadValidation/statusTransitions.json");
    }

    public StatusTransitionConfig(String configFilePath) throws IOException {
        try (InputStream inputStream = resolveConfigStream(configFilePath)) {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode configNode = objectMapper.readTree(inputStream);

            configNode.fields().forEachRemaining(entry -> {
                String requestType = entry.getKey();
                JsonNode statusMapNode = entry.getValue();
                Map<String, Set<String>> statusMap = new HashMap<>();

                statusMapNode.fields().forEachRemaining(statusEntry -> {
                    String currentStatus = statusEntry.getKey();
                    Set<String> validTransitions = new HashSet<>();
                    statusEntry.getValue().forEach(node -> validTransitions.add(node.asText()));
                    statusMap.put(currentStatus, validTransitions);
                });

                transitions.put(requestType, statusMap);
            });
        }
    }

    private InputStream resolveConfigStream(String configFilePath) throws IOException {
        String normalized = configFilePath.startsWith("/") ? configFilePath.substring(1) : configFilePath;
        ClassLoader cl = Optional.ofNullable(Thread.currentThread().getContextClassLoader())
                .orElse(StatusTransitionConfig.class.getClassLoader());

        InputStream inputStream = cl.getResourceAsStream(normalized);
        if (inputStream == null) {
            // Fall back to using the class loader of this class
            inputStream = StatusTransitionConfig.class.getClassLoader().getResourceAsStream(normalized);
        }
        if (inputStream == null) {
            // As a last resort, rely on Spring's ClassPathResource to locate the file
            Resource resource = new ClassPathResource(normalized);
            if (resource.exists()) {
                inputStream = resource.getInputStream();
            }
        }
        if (inputStream == null) {
            throw new FileNotFoundException("Status transition config not found on classpath: " + normalized);
        }
        return inputStream;
    }

    public boolean isValidTransition(String requestType, String currentStatus, String newStatus) {
        return transitions.containsKey(requestType) &&
                transitions.get(requestType).containsKey(currentStatus) &&
                transitions.get(requestType).get(currentStatus).contains(newStatus);
    }
}
