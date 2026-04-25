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
public class CaseFactsDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String caseType;

    private String caseTypeCode;

    private Map<String, String> facts;

    private String supplement;

    private List<String> keyPoints;

    private String summary;
}
