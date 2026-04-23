package com.feldsher.ai.skills.parent;

import com.feldsher.ai.skills.LegalSkill;
import com.feldsher.ai.tools.AskUserQuestionTool;
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
public class FactCollectionParentSkill implements LegalSkill {

    private final AskUserQuestionTool askUserQuestionTool;

    @Override
    public String getSkillCode() {
        return "fact_collection";
    }

    @Override
    public String getSkillName() {
        return "案情问题采集Skill";
    }

    @Override
    public String getSkillType() {
        return "parent";
    }

    @Override
    public String getPromptTemplate() {
        try {
            ClassPathResource resource = new ClassPathResource("prompts/skills/parent/fact_collection_skill.md");
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
            log.error("读取案情问题采集Skill提示词失败", e);
            return getDefaultPrompt();
        }
    }

    private String getDefaultPrompt() {
        return """
                # 案情问题采集Skill
                
                ## 角色定义
                你是一个专业的法律案情信息采集专家。你的核心职责是根据用户描述的法律问题，识别关键事实要素，设计有针对性的问题，引导用户提供完整的案情信息。
                
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
        log.info("执行案情问题采集Skill, sessionId: {}", context.getSessionId());

        String caseTypeCode = context.getCaseTypeCode();
        if (caseTypeCode == null) {
            caseTypeCode = "other";
        }

        askUserQuestionTool.setCurrentContext(context);
        AskUserQuestionTool.QuestionFormResult result = askUserQuestionTool.askUserQuestion(caseTypeCode, "");

        if (result.getSuccess() && result.getQuestionForm() != null) {
            return result.getQuestionForm();
        }

        throw new RuntimeException("生成问题表单失败: " + result.getErrorMessage());
    }
}
