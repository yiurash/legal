package com.feldsher.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SkillTypeEnum {

    FACT_COLLECTION_SKILL("案情问题采集Skill", "fact_collection"),
    LAW_RETRIEVAL_SKILL("法律法规检索Skill", "law_retrieval"),
    CASE_RETRIEVAL_SKILL("案例检索Skill", "case_retrieval"),
    MARRIAGE_FAMILY_SKILL("婚姻家事Skill", "marriage_family"),
    LABOR_DISPUTE_SKILL("劳务纠纷Skill", "labor_dispute"),
    CRIMINAL_PROCEDURE_SKILL("刑事诉讼Skill", "criminal_procedure"),
    CONTRACT_DISPUTE_SKILL("合同纠纷Skill", "contract_dispute");

    private final String description;
    private final String code;
}
