package com.feldsher.ai.service;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.PartialThinking;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealTimeStreamingService {

    private final StreamingChatModel streamingChatModel;

    public static class StreamingResult {
        private String thinkingContent;
        private String responseContent;
        private boolean success;
        private String errorMessage;

        public String getThinkingContent() { return thinkingContent; }
        public void setThinkingContent(String thinkingContent) { this.thinkingContent = thinkingContent; }
        public String getResponseContent() { return responseContent; }
        public void setResponseContent(String responseContent) { this.responseContent = responseContent; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }

    public interface StreamingCallback {
        void onThinkingStart();
        void onThinkingToken(String token);
        void onContentStart();
        void onContentToken(String token);
        void onComplete(StreamingResult result);
        void onError(Throwable error);
    }

    public static abstract class SimpleStreamingCallback implements StreamingCallback {
        @Override
        public void onThinkingStart() {}
        @Override
        public void onThinkingToken(String token) {}
        @Override
        public void onContentStart() {}
        @Override
        public void onContentToken(String token) {}
        @Override
        public void onComplete(StreamingResult result) {}
        @Override
        public void onError(Throwable error) {}
    }

    public void streamThinkingAndReply(
            String thinkingContent,
            String replyContent,
            StreamingCallback callback
    ) {
        StringBuilder thinkingBuilder = new StringBuilder();
        StringBuilder contentBuilder = new StringBuilder();

        callback.onThinkingStart();

        try {
            for (char c : thinkingContent.toCharArray()) {
                String token = String.valueOf(c);
                thinkingBuilder.append(token);
                callback.onThinkingToken(token);
                Thread.sleep(20);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        callback.onContentStart();

        try {
            for (char c : replyContent.toCharArray()) {
                String token = String.valueOf(c);
                contentBuilder.append(token);
                callback.onContentToken(token);
                Thread.sleep(30);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        StreamingResult result = new StreamingResult();
        result.setSuccess(true);
        result.setThinkingContent(thinkingBuilder.toString());
        result.setResponseContent(contentBuilder.toString());
        callback.onComplete(result);
    }

    public void streamChat(
            String systemPrompt,
            String userInput,
            StreamingCallback callback
    ) {
        log.info("开始真正的流式对话: userInput={}", userInput);

        StringBuilder thinkingBuilder = new StringBuilder();
        StringBuilder contentBuilder = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);

        try {
            String fullPrompt;
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                fullPrompt = systemPrompt + "\n\n用户问题：\n" + userInput;
            } else {
                fullPrompt = userInput;
            }

            streamingChatModel.chat(fullPrompt, new StreamingChatResponseHandler() {
                
                private boolean thinkingStarted = false;
                private boolean contentStarted = false;

                @Override
                public void onPartialResponse(String partialResponse) {
                    if (partialResponse == null || partialResponse.isEmpty()) {
                        return;
                    }

                    if (!contentStarted) {
                        callback.onContentStart();
                        contentStarted = true;
                    }
                    contentBuilder.append(partialResponse);
                    callback.onContentToken(partialResponse);
                }

                @Override
                public void onPartialThinking(PartialThinking partialThinking) {
                    if (partialThinking == null || partialThinking.text() == null || partialThinking.text().isEmpty()) {
                        return;
                    }

                    if (!thinkingStarted) {
                        callback.onThinkingStart();
                        thinkingStarted = true;
                    }
                    thinkingBuilder.append(partialThinking.text());
                    callback.onThinkingToken(partialThinking.text());
                }

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    log.info("流式响应完成: thinkingLength={}, contentLength={}", 
                            thinkingBuilder.length(), contentBuilder.length());
                    
                    StreamingResult result = new StreamingResult();
                    result.setSuccess(true);
                    result.setThinkingContent(thinkingBuilder.toString());
                    result.setResponseContent(contentBuilder.toString());
                    
                    callback.onComplete(result);
                    latch.countDown();
                }

                @Override
                public void onError(Throwable error) {
                    log.error("流式响应出错", error);
                    callback.onError(error);
                    latch.countDown();
                }
            });

            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("流式等待被中断");
            }

        } catch (Exception e) {
            log.error("流式对话出错", e);
            callback.onError(e);
        }
    }

    public StreamingResult chatSync(
            String systemPrompt,
            String userInput
    ) {
        final StreamingResult[] resultHolder = new StreamingResult[1];
        final CountDownLatch latch = new CountDownLatch(1);

        streamChat(systemPrompt, userInput, new SimpleStreamingCallback() {
            @Override
            public void onComplete(StreamingResult result) {
                resultHolder[0] = result;
                latch.countDown();
            }

            @Override
            public void onError(Throwable error) {
                StreamingResult result = new StreamingResult();
                result.setSuccess(false);
                result.setErrorMessage(error.getMessage());
                resultHolder[0] = result;
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return resultHolder[0];
    }

    public void generateReplyWithStreaming(
            String systemPrompt,
            String userInput,
            Consumer<String> onThinking,
            Consumer<String> onContent,
            Runnable onComplete
    ) {
        streamChat(systemPrompt, userInput, new SimpleStreamingCallback() {
            @Override
            public void onThinkingToken(String token) {
                if (onThinking != null) {
                    onThinking.accept(token);
                }
            }

            @Override
            public void onContentToken(String token) {
                if (onContent != null) {
                    onContent.accept(token);
                }
            }

            @Override
            public void onComplete(StreamingResult result) {
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
    }

    public void analyzeUserIntent(
            String userInput,
            StreamingCallback callback
    ) {
        String systemPrompt = """
你是一个专业的法律助手。请仔细分析用户的问题。

## 任务
1. 首先判断用户的问题是否与法律相关
2. 如果是非法律问题（如纯数字、无意义字符、闲聊等），请直接回复："亲，我只能回答法律方面问题哦~"
3. 如果是法律问题，请按照以下流程处理：
   - 分析问题类型和案由
   - 决定是否需要采集更多信息
   - 给出你的分析和建议

## 核心原则
- 对于纯数字（如"11"、"123"）、纯字母、特殊符号组合等无意义输入，直接返回固定回复
- 对于"今天天气怎么样"、"你好"等非法律闲聊，也返回固定回复
- 只有明确涉及法律问题的内容才进行正常处理

## 思考输出
请在思考过程中展示你的分析过程。
""";

        streamChat(systemPrompt, userInput, callback);
    }
}
