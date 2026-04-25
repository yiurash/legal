package com.feldsher.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromptProvider {

    private final ResourceLoader resourceLoader;

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    @Value("${prompt.hot-reload:false}")
    private boolean hotReload;

    public String getPrompt(String key, String fallback) {
        if (hotReload) {
            return loadPrompt(key, fallback);
        }
        return cache.computeIfAbsent(key, k -> loadPrompt(k, fallback));
    }

    private String loadPrompt(String key, String fallback) {
        String path = "classpath:prompts/" + key + ".md";
        try {
            Resource resource = resourceLoader.getResource(path);
            if (!resource.exists()) {
                log.warn("Prompt文件不存在，使用fallback。prompt_key={}", key);
                return fallback;
            }

            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }
            log.info("加载Prompt成功。prompt_key={}", key);
            return content.toString();
        } catch (Exception e) {
            log.error("加载Prompt失败，使用fallback。prompt_key={}", key, e);
            return fallback;
        }
    }
}

