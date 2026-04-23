package com.feldsher.ai.skills;

import com.feldsher.common.context.DialogueContext;

public interface LegalSkill {

    String getSkillCode();

    String getSkillName();

    String getSkillType();

    String getPromptTemplate();

    Object execute(DialogueContext context, Object... args);
}
