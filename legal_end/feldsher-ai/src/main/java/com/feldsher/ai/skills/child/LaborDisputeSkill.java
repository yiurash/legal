package com.feldsher.ai.skills.child;

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
public class LaborDisputeSkill implements LegalSkill {

    private final AskUserQuestionTool askUserQuestionTool;

    @Override
    public String getSkillCode() {
        return "labor_dispute";
    }

    @Override
    public String getSkillName() {
        return "劳务纠纷Skill";
    }

    @Override
    public String getSkillType() {
        return "child";
    }

    @Override
    public String getPromptTemplate() {
        try {
            ClassPathResource resource = new ClassPathResource("prompts/skills/child/labor_dispute_skill.md");
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
            log.error("读取劳务纠纷Skill提示词失败", e);
            return getDefaultPrompt();
        }
    }

    private String getDefaultPrompt() {
        return """
                # 劳务纠纷Skill
                
                ## 适用案由
                - 劳动合同纠纷
                - 确认劳动关系纠纷
                - 劳务派遣合同纠纷
                - 非全日制用工纠纷
                - 追索劳动报酬纠纷
                - 经济补偿金纠纷
                - 竞业限制纠纷
                - 社会保险纠纷
                - 工伤认定相关纠纷
                
                ## 核心问题
                1. 用工关系：是否签订书面劳动合同、劳务派遣、非全日制等
                2. 纠纷类型：拖欠工资、违法解除、未缴社保、工伤等
                3. 工资情况：月工资标准、工资构成
                4. 工作时间：标准工时、经常加班、不定时工作制等
                5. 证据情况：劳动合同、工资记录、工作证、考勤记录等
                6. 维权进展：是否已协商、投诉、仲裁、诉讼
                
                ## 常用法律法规
                - 《劳动合同法》
                - 《劳动法》
                - 《劳动争议调解仲裁法》
                - 《工伤保险条例》
                
                ## 重要时效规定
                - 劳动争议仲裁时效：1年
                - 工伤认定申请时效：单位30日内，职工1年内
                - 未签劳动合同二倍工资：最多11个月
                
                ## 典型案例要点
                - 未签劳动合同需支付二倍工资，最多11个月
                - 因拖欠工资离职的，可主张经济补偿
                - 违法解除劳动合同需支付赔偿金（经济补偿的2倍）
                - 规章制度需经过民主程序制定并公示
                """;
    }

    @Override
    public QuestionFormDTO execute(DialogueContext context, Object... args) {
        log.info("执行劳务纠纷Skill, sessionId: {}", context.getSessionId());

        askUserQuestionTool.setCurrentContext(context);
        AskUserQuestionTool.QuestionFormResult result = askUserQuestionTool.askUserQuestion("labor_dispute", "");

        if (result.getSuccess() && result.getQuestionForm() != null) {
            return result.getQuestionForm();
        }

        throw new RuntimeException("生成问题表单失败: " + result.getErrorMessage());
    }
}
