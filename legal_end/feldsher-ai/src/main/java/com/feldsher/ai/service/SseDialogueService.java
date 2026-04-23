package com.feldsher.ai.service;

import com.alibaba.fastjson2.JSON;
import com.feldsher.ai.agents.MasterAgent;
import com.feldsher.common.context.DialogueContext;
import com.feldsher.common.enums.DialogueStatusEnum;
import com.feldsher.common.vo.DialogueResponseVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseDialogueService {

    private final DialogueService dialogueService;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    private final Map<String, SseEmitter> emitterStore = new java.util.concurrent.ConcurrentHashMap<>();

    public SseEmitter createConnection(String sessionId) {
        SseEmitter emitter = new SseEmitter(300000L);

        emitter.onCompletion(() -> {
            log.info("SSE连接完成: {}", sessionId);
            emitterStore.remove(sessionId);
        });

        emitter.onTimeout(() -> {
            log.info("SSE连接超时: {}", sessionId);
            emitterStore.remove(sessionId);
        });

        emitter.onError(e -> {
            log.error("SSE连接错误: {}", sessionId, e);
            emitterStore.remove(sessionId);
        });

        emitterStore.put(sessionId, emitter);
        log.info("创建SSE连接: {}", sessionId);

        return emitter;
    }

    public void startDialogue(String sessionId, String userInput) {
        SseEmitter emitter = emitterStore.get(sessionId);
        if (emitter == null) {
            log.warn("SSE连接不存在: {}", sessionId);
            return;
        }

        executor.execute(() -> {
            try {
                DialogueContext context = dialogueService.getContext(sessionId);
                if (context == null) {
                    context = dialogueService.createNewSession();
                }

                sendThinkingStart(emitter, sessionId);

                MasterAgent.MasterAgentResult result = dialogueService.processUserInput(sessionId, userInput);

                if (result.getThinkingContent() != null) {
                    sendThinkingContent(emitter, result.getThinkingContent());
                }

                if (result.getSuccess()) {
                    if (result.getQuestionForm() != null) {
                        sendQuestionForm(emitter, result.getQuestionForm());
                    } else if (result.getConclusion() != null) {
                        sendConclusion(emitter, result.getConclusion());
                    }
                } else {
                    sendError(emitter, result.getErrorMessage());
                }

                sendComplete(emitter, result.getIsComplete() != null && result.getIsComplete());

            } catch (Exception e) {
                log.error("处理对话出错: {}", sessionId, e);
                try {
                    sendError(emitter, "处理出错: " + e.getMessage());
                } catch (IOException ex) {
                    log.error("发送错误消息失败", ex);
                }
            }
        });
    }

    public void submitAnswers(String sessionId, Map<String, String> answers, String supplement) {
        SseEmitter emitter = emitterStore.get(sessionId);
        if (emitter == null) {
            log.warn("SSE连接不存在: {}", sessionId);
            return;
        }

        executor.execute(() -> {
            try {
                sendThinkingStart(emitter, sessionId);

                MasterAgent.MasterAgentResult result = dialogueService.processUserAnswers(sessionId, answers, supplement);

                if (result.getThinkingContent() != null) {
                    sendThinkingContent(emitter, result.getThinkingContent());
                }

                if (result.getSuccess()) {
                    if (result.getConclusion() != null) {
                        sendConclusion(emitter, result.getConclusion());
                    }
                } else {
                    sendError(emitter, result.getErrorMessage());
                }

                sendComplete(emitter, result.getIsComplete() != null && result.getIsComplete());

            } catch (Exception e) {
                log.error("处理回答出错: {}", sessionId, e);
                try {
                    sendError(emitter, "处理出错: " + e.getMessage());
                } catch (IOException ex) {
                    log.error("发送错误消息失败", ex);
                }
            }
        });
    }

    private void sendThinkingStart(SseEmitter emitter, String sessionId) throws IOException {
        var event = SseEmitter.event()
                .name("thinking_start")
                .data(JSON.toJSONString(Map.of(
                        "sessionId", sessionId,
                        "message", "正在分析您的问题..."
                )));
        emitter.send(event);
    }

    private void sendThinkingContent(SseEmitter emitter, String content) throws IOException {
        String[] chars = content.split("");
        for (int i = 0; i < chars.length; i++) {
            var event = SseEmitter.event()
                    .name("thinking")
                    .data(JSON.toJSONString(Map.of(
                            "content", chars[i],
                            "index", i,
                            "total", chars.length
                    )));
            emitter.send(event);
            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void sendQuestionForm(SseEmitter emitter, com.feldsher.common.dto.QuestionFormDTO questionForm) throws IOException {
        var event = SseEmitter.event()
                .name("question_form")
                .data(JSON.toJSONString(questionForm));
        emitter.send(event);
    }

    private void sendConclusion(SseEmitter emitter, DialogueResponseVO.FinalConclusion conclusion) throws IOException {
        sendTextContent(emitter, "case_analysis", conclusion.getCaseAnalysis());
        
        var lawEvent = SseEmitter.event()
                .name("law_results")
                .data(JSON.toJSONString(conclusion.getLawResults()));
        emitter.send(lawEvent);
        
        var caseEvent = SseEmitter.event()
                .name("case_results")
                .data(JSON.toJSONString(conclusion.getCaseResults()));
        emitter.send(caseEvent);
        
        sendTextContent(emitter, "summary", conclusion.getSummary());
        sendTextContent(emitter, "next_steps", conclusion.getNextSteps());
    }

    private void sendTextContent(SseEmitter emitter, String type, String content) throws IOException {
        if (content == null || content.isEmpty()) {
            return;
        }

        String[] lines = content.split("\n");
        for (String line : lines) {
            if (line.isEmpty()) {
                var event = SseEmitter.event()
                        .name(type)
                        .data(JSON.toJSONString(Map.of(
                                "content", "\n",
                                "isEnd", false
                        )));
                emitter.send(event);
                continue;
            }

            String[] chars = line.split("");
            for (int i = 0; i < chars.length; i++) {
                var event = SseEmitter.event()
                        .name(type)
                        .data(JSON.toJSONString(Map.of(
                                "content", chars[i],
                                "isEnd", false
                        )));
                emitter.send(event);
                try {
                    Thread.sleep(40);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            var lineEndEvent = SseEmitter.event()
                    .name(type)
                    .data(JSON.toJSONString(Map.of(
                            "content", "\n",
                            "isEnd", false
                    )));
            emitter.send(lineEndEvent);
        }
    }

    private void sendError(SseEmitter emitter, String errorMessage) throws IOException {
        var event = SseEmitter.event()
                .name("error")
                .data(JSON.toJSONString(Map.of(
                        "message", errorMessage
                )));
        emitter.send(event);
    }

    private void sendComplete(SseEmitter emitter, boolean isComplete) throws IOException {
        var event = SseEmitter.event()
                .name("complete")
                .data(JSON.toJSONString(Map.of(
                        "isComplete", isComplete,
                        "message", isComplete ? "对话已完成" : "等待用户输入"
                )));
        emitter.send(event);
    }

    public void closeConnection(String sessionId) {
        SseEmitter emitter = emitterStore.remove(sessionId);
        if (emitter != null) {
            emitter.complete();
            log.info("关闭SSE连接: {}", sessionId);
        }
        dialogueService.removeSession(sessionId);
    }
}
