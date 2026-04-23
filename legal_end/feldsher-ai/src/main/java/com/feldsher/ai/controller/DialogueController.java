package com.feldsher.ai.controller;

import com.feldsher.ai.service.DialogueService;
import com.feldsher.ai.service.SseDialogueService;
import com.feldsher.common.context.DialogueContext;
import com.feldsher.common.request.DialogueRequest;
import com.feldsher.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/dialogue")
@RequiredArgsConstructor
public class DialogueController {

    private final DialogueService dialogueService;
    private final SseDialogueService sseDialogueService;

    @PostMapping("/new")
    public Result<DialogueContext> createNewSession() {
        log.info("创建新对话会话");
        DialogueContext context = dialogueService.createNewSession();
        return Result.success(context);
    }

    @GetMapping(value = "/sse/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(@RequestParam String sessionId) {
        log.info("建立SSE连接, sessionId: {}", sessionId);
        return sseDialogueService.createConnection(sessionId);
    }

    @PostMapping("/send")
    public Result<Void> sendMessage(@Valid @RequestBody DialogueRequest request) {
        log.info("发送消息, sessionId: {}, input: {}", request.getSessionId(), request.getUserInput());

        if (request.getSessionId() == null || request.getSessionId().isEmpty()) {
            DialogueContext context = dialogueService.createNewSession();
            request.setSessionId(context.getSessionId());
        }

        sseDialogueService.startDialogue(request.getSessionId(), request.getUserInput());

        return Result.success();
    }

    @PostMapping("/answers")
    public Result<Void> submitAnswers(
            @RequestParam String sessionId,
            @RequestBody(required = false) Map<String, Object> requestBody) {
        
        log.info("提交回答, sessionId: {}", sessionId);

        @SuppressWarnings("unchecked")
        Map<String, String> answers = (Map<String, String>) requestBody.get("answers");
        String supplement = (String) requestBody.get("supplement");

        if (answers == null) {
            answers = Map.of();
        }

        sseDialogueService.submitAnswers(sessionId, answers, supplement);

        return Result.success();
    }

    @PostMapping("/close")
    public Result<Void> closeSession(@RequestParam String sessionId) {
        log.info("关闭对话会话: {}", sessionId);
        sseDialogueService.closeConnection(sessionId);
        return Result.success();
    }

    @GetMapping("/context")
    public Result<DialogueContext> getContext(@RequestParam String sessionId) {
        log.info("获取对话上下文, sessionId: {}", sessionId);
        DialogueContext context = dialogueService.getContext(sessionId);
        if (context == null) {
            return Result.error("会话不存在或已过期");
        }
        return Result.success(context);
    }
}
