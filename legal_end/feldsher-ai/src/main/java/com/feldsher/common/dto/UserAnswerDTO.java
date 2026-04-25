package com.feldsher.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAnswerDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String sessionId;

    private Map<String, String> selectedAnswers;

    private String supplement;

    private List<String> keyPoints;
}
