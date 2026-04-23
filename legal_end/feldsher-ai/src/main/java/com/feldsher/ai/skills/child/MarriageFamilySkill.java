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
public class MarriageFamilySkill implements LegalSkill {

    private final AskUserQuestionTool askUserQuestionTool;

    @Override
    public String getSkillCode() {
        return "marriage_family";
    }

    @Override
    public String getSkillName() {
        return "婚姻家事Skill";
    }

    @Override
    public String getSkillType() {
        return "child";
    }

    @Override
    public String getPromptTemplate() {
        try {
            ClassPathResource resource = new ClassPathResource("prompts/skills/child/marriage_family_skill.md");
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
            log.error("读取婚姻家事Skill提示词失败", e);
            return getDefaultPrompt();
        }
    }

    private String getDefaultPrompt() {
        return """
                # 婚姻家事Skill
                
                ## 适用案由
                - 离婚纠纷
                - 婚约财产纠纷
                - 夫妻财产约定纠纷
                - 同居关系纠纷
                - 抚养纠纷
                - 扶养纠纷
                - 赡养纠纷
                - 继承纠纷
                
                ## 核心问题
                1. 婚姻状态：已婚、分居、已离婚
                2. 子女情况：数量、年龄、目前生活状况
                3. 财产情况：房产、存款、车辆、公司股权等
                4. 离婚原因：感情不和、出轨、家暴、赌博等
                5. 证据情况：掌握哪些证据材料
                6. 诉讼进展：是否已起诉、是否已判决
                
                ## 常用法律法规
                - 《民法典》婚姻家庭编（第1040-1118条）
                - 最高人民法院关于适用《民法典》婚姻家庭编的解释（一）
                
                ## 典型案例要点
                - 因感情不和分居满两年是法定离婚情形
                - 子女抚养权优先考虑最有利于未成年子女的原则
                - 不满两周岁的子女以由母亲直接抚养为原则
                - 夫妻共同财产原则上平均分割，照顾子女、女方和无过错方
                """;
    }

    @Override
    public QuestionFormDTO execute(DialogueContext context, Object... args) {
        log.info("执行婚姻家事Skill, sessionId: {}", context.getSessionId());

        askUserQuestionTool.setCurrentContext(context);
        AskUserQuestionTool.QuestionFormResult result = askUserQuestionTool.askUserQuestion("marriage_family", "");

        if (result.getSuccess() && result.getQuestionForm() != null) {
            return result.getQuestionForm();
        }

        throw new RuntimeException("生成问题表单失败: " + result.getErrorMessage());
    }
}
