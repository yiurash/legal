package com.feldsher.common.context;

import com.feldsher.common.dto.CaseFactsDTO;
import com.feldsher.common.dto.QuestionFormDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DialogueContext implements Serializable {

    private static final long serialVersionUID = 1L;

    private String sessionId;

    private String userId;

    private String caseType;

    private String caseTypeCode;

    private String currentStatus;

    private String currentAgent;

    private String currentSkill;

    private List<Message> messages;

    private CaseFactsDTO caseFacts;

    private QuestionFormDTO pendingQuestions;

    private Map<String, Object> extraData;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message implements Serializable {
        private static final long serialVersionUID = 1L;

        private String role;

        private String content;

        private String type;

        private LocalDateTime timestamp;
    }

    public void addMessage(String role, String content, String type) {
        if (this.messages == null) {
            this.messages = new ArrayList<>();
        }
        this.messages.add(Message.builder()
                .role(role)
                .content(content)
                .type(type)
                .timestamp(LocalDateTime.now())
                .build());
    }
}
