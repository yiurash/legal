package com.feldsher.ai.service;

import com.feldsher.ai.agents.AgentRegistry;
import com.feldsher.ai.agents.MasterAgent;
import com.feldsher.common.context.DialogueContext;
import com.feldsher.common.enums.DialogueStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class DialogueService {

    private final AgentRegistry agentRegistry;

    private final Map<String, DialogueContext> contextStore = new ConcurrentHashMap<>();

    public DialogueContext createNewSession() {
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        
        DialogueContext context = DialogueContext.builder()
                .sessionId(sessionId)
                .currentStatus(DialogueStatusEnum.INIT.getCode())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        contextStore.put(sessionId, context);
        log.info("创建新对话会话: {}", sessionId);

        return context;
    }

    public DialogueContext getContext(String sessionId) {
        return contextStore.get(sessionId);
    }

    public void updateContext(String sessionId, DialogueContext context) {
        context.setUpdatedAt(LocalDateTime.now());
        contextStore.put(sessionId, context);
    }

    public void removeSession(String sessionId) {
        contextStore.remove(sessionId);
        log.info("移除对话会话: {}", sessionId);
    }

    public MasterAgent.MasterAgentResult processUserInput(String sessionId, String userInput) {
        log.info("处理用户输入, sessionId: {}, input: {}", sessionId, userInput);

        DialogueContext context = getContext(sessionId);
        if (context == null) {
            throw new RuntimeException("会话不存在或已过期: " + sessionId);
        }

        if (isMeaninglessInput(userInput)) {
            log.warn("检测到无意义输入或非法律问题: {}", userInput);
            return MasterAgent.MasterAgentResult.builder()
                    .success(true)
                    .isNonLegalQuestion(true)
                    .thinkingContent("亲，我只能回答法律方面问题哦~")
                    .build();
        }

        context.addMessage("user", userInput, "text");

        MasterAgent masterAgent = agentRegistry.getMasterAgent();
        MasterAgent.MasterAgentResult result = (MasterAgent.MasterAgentResult) masterAgent.execute(context, userInput);

        updateContext(sessionId, context);

        log.info("处理完成, sessionId: {}, success: {}", sessionId, result.getSuccess());
        return result;
    }

    public MasterAgent.MasterAgentResult processUserAnswers(String sessionId, Map<String, String> answers, String supplement) {
        log.info("处理用户回答, sessionId: {}", sessionId);

        DialogueContext context = getContext(sessionId);
        if (context == null) {
            throw new RuntimeException("会话不存在或已过期: " + sessionId);
        }

        var answerJson = new com.alibaba.fastjson2.JSONObject();
        answerJson.put("answers", answers);
        answerJson.put("supplement", supplement);

        String jsonInput = answerJson.toJSONString();
        log.info("用户回答JSON: {}", jsonInput);

        return processUserInput(sessionId, jsonInput);
    }

    public boolean isMeaninglessInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return true;
        }
        
        String trimmed = input.trim();
        
        if (trimmed.matches("^\\d+$")) {
            return true;
        }
        
        if (trimmed.matches("^[a-zA-Z]+$")) {
            return true;
        }
        
        if (trimmed.matches("^[\\s\\p{Punct}]+$")) {
            return true;
        }
        
        if (trimmed.length() < 2) {
            return true;
        }
        
        if (trimmed.length() <= 5 && !hasLegalKeywords(trimmed)) {
            return true;
        }
        
        return false;
    }

    public boolean hasLegalKeywords(String input) {
        String lower = input.toLowerCase();
        return lower.contains("法律") || lower.contains("法") || lower.contains("律")
                || lower.contains("律师") || lower.contains("法院") || lower.contains("诉讼")
                || lower.contains("合同") || lower.contains("离婚") || lower.contains("劳动")
                || lower.contains("纠纷") || lower.contains("赔偿") || lower.contains("欠款")
                || lower.contains("钱") || lower.contains("债") || lower.contains("婚")
                || lower.contains("伤") || lower.contains("罪") || lower.contains("骗")
                || lower.contains("咨询") || lower.contains("问题") || lower.contains("怎么办")
                || lower.contains("怎么处理") || lower.contains("应该") || lower.contains("可以");
    }
}
