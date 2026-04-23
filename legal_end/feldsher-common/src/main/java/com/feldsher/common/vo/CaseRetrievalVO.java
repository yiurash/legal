package com.feldsher.common.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseRetrievalVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String caseId;

    private String caseName;

    private String court;

    private String caseNumber;

    private LocalDate judgmentDate;

    private String caseType;

    private String basicFacts;

    private List<String> disputeFocus;

    private String courtFinding;

    private String judgmentResult;

    private Double relevanceScore;

    private String relevanceAnalysis;

    private List<String> keyTakeaways;
}
