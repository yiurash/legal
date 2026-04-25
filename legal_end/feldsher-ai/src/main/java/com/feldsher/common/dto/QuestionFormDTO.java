package com.feldsher.common.dto;

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
public class QuestionFormDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String caseType;

    private String caseTypeCode;

    private List<QuestionItem> questions;

    private Boolean hasSupplement;

    private String supplementHint;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionItem implements Serializable {
        private static final long serialVersionUID = 1L;

        private String id;

        private String question;

        private List<String> options;

        private Integer order;

        private String followUp;
    }
}
