package com.feldsher.common.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LawRetrievalVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String lawId;

    private String lawName;

    private String articleNumber;

    private String articleTitle;

    private String content;

    private String relevanceAnalysis;

    private String keyPoints;
}
