package com.feldsher.ai.agents;

import com.feldsher.ai.skills.LegalSkill;
import com.feldsher.ai.skills.SkillRegistry;
import com.feldsher.common.context.DialogueContext;
import com.feldsher.common.dto.QuestionFormDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class FactCollectionAgent implements LegalAgent {

    private final SkillRegistry skillRegistry;

    @Override
    public String getAgentCode() {
        return "fact_collection";
    }

    @Override
    public String getAgentName() {
        return "案情问题采集Agent";
    }

    @Override
    public String getPromptTemplate() {
        try {
            ClassPathResource resource = new ClassPathResource("prompts/agents/fact_collection_agent.md");
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
                return content.toString();
            }
        } catch (Exception e) {
            log.error("读取案情问题采集Agent提示词失败", e);
            return getDefaultPrompt();
        }
    }

    private String getDefaultPrompt() {
        return """
                # 案情问题采集Agent
                
                ## 角色定义
                你是一个专业的法律案情信息采集Agent。你的核心职责是：
                1. 根据用户描述的法律问题，识别关键事实要素
                2. 设计有针对性的问题，引导用户提供完整的案情信息
                3. 整理用户的回答，形成结构化的案情事实档案
                
                ## 问题设计原则
                1. 相关性：每个问题必须与案件直接相关
                2. 层次性：先问核心事实，再问细节补充
                3. 可选性：每个问题提供2-4个选项，最后一个选项为"暂不选择"
                4. 开放性：在选项后提供补充说明的输入框
                
                ## 问题数量控制
                - 简单案件：3-4个问题
                - 复杂案件：5-6个问题
                - 绝不超过6个问题
                """;
    }

    @Override
    public QuestionFormDTO execute(DialogueContext context, Object... args) {
        log.info("案情问题采集Agent开始执行, sessionId: {}", context.getSessionId());

        String caseTypeCode = context.getCaseTypeCode();
        if (caseTypeCode == null) {
            caseTypeCode = "other";
        }

        LegalSkill skill = skillRegistry.getSkillByCaseType(caseTypeCode);
        if (skill == null) {
            skill = skillRegistry.getSkill("fact_collection");
        }

        log.info("使用Skill: {} - {}", skill.getSkillCode(), skill.getSkillName());

        Object result = skill.execute(context, args);
        if (result instanceof QuestionFormDTO questionForm) {
            context.setPendingQuestions(questionForm);
            return questionForm;
        }

        throw new RuntimeException("生成问题表单失败");
    }
}
