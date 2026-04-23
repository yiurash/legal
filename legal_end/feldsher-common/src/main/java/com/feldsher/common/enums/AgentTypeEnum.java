package com.feldsher.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AgentTypeEnum {

    MASTER_AGENT("主Agent", "master"),
    FACT_COLLECTION_AGENT("案情问题采集Agent", "fact_collection"),
    LAW_RETRIEVAL_AGENT("法律法规检索Agent", "law_retrieval"),
    CASE_RETRIEVAL_AGENT("案例检索Agent", "case_retrieval");

    private final String description;
    private final String code;
}
