package com.feldsher.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DialogueStatusEnum {

    INIT("初始化", "init"),
    INTENTION_ANALYZING("意图分析中", "intention_analyzing"),
    QUESTION_COLLECTING("问题采集中", "question_collecting"),
    LAW_SEARCHING("法条检索中", "law_searching"),
    CASE_SEARCHING("案例检索中", "case_searching"),
    CONCLUDING("结论生成中", "concluding"),
    COMPLETED("已完成", "completed"),
    ERROR("异常", "error");

    private final String description;
    private final String code;
}
