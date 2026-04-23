package com.feldsher.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntentionResultDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String intention;

    private String caseType;

    private String caseTypeCode;

    private Boolean needMoreInfo;

    private Double confidence;

    private String nextAgent;

    private String nextSkill;
}
