package com.feldsher.ai.agents;

import com.feldsher.common.context.DialogueContext;

public interface LegalAgent {

    String getAgentCode();

    String getAgentName();

    String getPromptTemplate();

    Object execute(DialogueContext context, Object... args);
}
