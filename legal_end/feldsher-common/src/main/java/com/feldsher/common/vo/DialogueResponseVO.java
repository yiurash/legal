package com.feldsher.common.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DialogueResponseVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String sessionId;

    private String status;

    private String type;

    private Object data;

    private String thinkingContent;

    private String content;

    private Boolean isComplete;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinalConclusion implements Serializable {
        private static final long serialVersionUID = 1L;

        private String caseAnalysis;

        private List<LawRetrievalVO> lawResults;

        private List<CaseRetrievalVO> caseResults;

        private String summary;

        private String nextSteps;
    }
}
