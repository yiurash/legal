package com.feldsher.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CaseTypeEnum {

    MARRIAGE_FAMILY("婚姻家事", "marriage_family"),
    LABOR_DISPUTE("劳务纠纷", "labor_dispute"),
    CRIMINAL_PROCEDURE("刑事诉讼", "criminal_procedure"),
    CONTRACT_DISPUTE("合同纠纷", "contract_dispute"),
    DEBT_DISPUTE("债权债务", "debt_dispute"),
    OTHER("其他", "other");

    private final String description;
    private final String code;

    public static CaseTypeEnum getByCode(String code) {
        for (CaseTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return OTHER;
    }
}
