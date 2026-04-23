package com.feldsher.ai.service;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class LLMDialogueService {

    private final StreamingChatModel streamingChatModel;

    private final ConcurrentHashMap<String, List<ChatMessage>> sessionContexts = new ConcurrentHashMap<>();

    private static final String SYSTEM_PROMPT = """
你是一个专业的法律助手AI，专门帮助用户解决法律问题。

## 你的角色
1. **意图识别**：首先分析用户的问题是否与法律相关
2. **案由识别**：如果是法律问题，识别属于哪个法律领域
3. **信息采集**：根据案件类型，向用户提出针对性的问题（3-6个问题）
4. **法律分析**：基于收集的信息，给出法律建议

## 核心原则

### 1. 非法律问题识别
如果用户输入以下内容，直接回复："亲，我只能回答法律方面问题哦~"
- 纯数字（如"11"、"123"）
- 纯字母
- 无意义字符
- 非法律闲聊（如"今天天气怎么样"、"你好"）
- 与法律无关的问题

### 2. 案由分类
如果是法律问题，识别以下类型之一：
- **婚姻家事**：离婚、财产分割、子女抚养、彩礼、遗产继承等
- **劳务纠纷**：工资拖欠、劳动合同、辞退、社保、工伤等
- **刑事诉讼**：诈骗、盗窃、抢劫、故意伤害、醉驾、取保候审等
- **合同纠纷**：民间借贷、买卖合同、租赁合同、违约责任等
- **其他**：无法归为以上类别的法律问题

### 3. 问题采集原则
- 每个案件类型提出 **3-6个问题**
- 每个问题提供 **2-4个选项**
- 最后一个选项始终是"暂不选择"
- 询问完所有选项后，还要问用户是否有补充说明

### 4. 回复格式要求

当需要向用户提问时，请使用以下 JSON 格式回复：
```json
{
  "action": "ASK_QUESTIONS",
  "caseType": "刑事诉讼",
  "caseTypeCode": "criminal_procedure",
  "questions": [
    {
      "id": "q1",
      "question": "目前案件处于什么阶段？",
      "options": ["只是被询问/调查，还未立案", "公安机关侦查阶段", "检察机关审查起诉阶段", "暂不选择"]
    }
  ],
  "thinking": "根据用户描述，这涉及诈骗行为，属于刑事诉讼范畴。我需要了解案件的具体阶段等信息。"
}
```

当用户回答完所有问题，你可以给出法律建议时，请使用以下 JSON 格式：
```json
{
  "action": "GIVE_ADVICE",
  "caseAnalysis": "根据您提供的情况...",
  "laws": [
    {
      "lawName": "中华人民共和国刑法",
      "articleNumber": "第二百六十六条",
      "content": "诈骗公私财物，数额较大的，处三年以下有期徒刑、拘役或者管制..."
    }
  ],
  "summary": "综合以上分析...",
  "nextSteps": "1. 建议您首先...",
  "thinking": "基于用户提供的信息，我进行以下法律分析..."
}
```

如果识别为非法律问题，请使用以下格式：
```json
{
  "action": "NON_LEGAL",
  "message": "亲，我只能回答法律方面问题哦~"
}
```

## 重要提示
1. 始终使用 JSON 格式回复
2. 如果不确定，先提问，而不是直接给出建议
3. 保持专业、客观的态度
4. 分析时要基于法律规定，而不是主观判断
5. 明确告知用户：以上分析仅供参考，不构成正式法律意见
""";

    public String createNewSession() {
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new SystemMessage(SYSTEM_PROMPT));
        sessionContexts.put(sessionId, messages);
        log.info("创建新对话会话: {}", sessionId);
        return sessionId;
    }

    public void removeSession(String sessionId) {
        sessionContexts.remove(sessionId);
        log.info("移除对话会话: {}", sessionId);
    }

    public boolean hasSession(String sessionId) {
        return sessionContexts.containsKey(sessionId);
    }

    public void chatWithStreaming(String sessionId, String userMessage, StreamingCallback callback) {
        log.info("开始流式对话, sessionId: {}, userMessage: {}", sessionId, userMessage);

        List<ChatMessage> messages = sessionContexts.get(sessionId);
        if (messages == null) {
            callback.onError(new RuntimeException("会话不存在或已过期: " + sessionId));
            return;
        }

        messages.add(new UserMessage(userMessage));

        StringBuilder fullResponse = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        try {
            callback.onStart();

            streamingChatModel.chat(messages, new StreamingChatResponseHandler() {
                
                private boolean firstToken = true;

                @Override
                public void onPartialResponse(String partialResponse) {
                    if (partialResponse == null || partialResponse.isEmpty()) {
                        return;
                    }
                    
                    if (firstToken) {
                        callback.onFirstToken();
                        firstToken = false;
                    }
                    
                    fullResponse.append(partialResponse);
                    callback.onToken(partialResponse);
                }

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    AiMessage aiMessage = completeResponse.aiMessage();
                    if (aiMessage != null && aiMessage.text() != null) {
                        messages.add(new AiMessage(aiMessage.text()));
                    }
                    
                    log.info("流式响应完成, responseLength: {}", fullResponse.length());
                    callback.onComplete(fullResponse.toString());
                    latch.countDown();
                }

                @Override
                public void onError(Throwable error) {
                    log.error("流式响应出错", error);
                    errorRef.set(error);
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

    public interface StreamingCallback {
        void onStart();
        void onFirstToken();
        void onToken(String token);
        void onComplete(String fullResponse);
        void onError(Throwable error);
    }

    public static abstract class SimpleStreamingCallback implements StreamingCallback {
        @Override
        public void onStart() {}
        @Override
        public void onFirstToken() {}
        @Override
        public void onToken(String token) {}
        @Override
        public void onComplete(String fullResponse) {}
        @Override
        public void onError(Throwable error) {}
    }
}
