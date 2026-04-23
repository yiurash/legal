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
public class CriminalProcedureSkill implements LegalSkill {

    private final AskUserQuestionTool askUserQuestionTool;

    @Override
    public String getSkillCode() {
        return "criminal_procedure";
    }

    @Override
    public String getSkillName() {
        return "刑事诉讼Skill";
    }

    @Override
    public String getSkillType() {
        return "child";
    }

    @Override
    public String getPromptTemplate() {
        try {
            ClassPathResource resource = new ClassPathResource("prompts/skills/child/criminal_procedure_skill.md");
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
            log.error("读取刑事诉讼Skill提示词失败", e);
            return getDefaultPrompt();
        }
    }

    private String getDefaultPrompt() {
        return """
                # 刑事诉讼Skill
                
                ## 适用场景
                - 被公安机关传唤/讯问
                - 被采取强制措施（取保候审、监视居住、拘留、逮捕）
                - 涉嫌刑事犯罪需要辩护
                - 作为被害人需要维权
                - 刑事附带民事诉讼
                - 申诉、再审申请
                
                ## 常见罪名
                - 诈骗罪、合同诈骗罪
                - 故意伤害罪、寻衅滋事罪
                - 盗窃罪、抢劫罪
                - 危险驾驶罪（醉驾）
                - 非法吸收公众存款罪
                - 开设赌场罪
                - 交通肇事罪
                - 强奸罪
                - 职务侵占罪、挪用资金罪
                
                ## 核心问题
                1. 案件阶段：未立案、侦查阶段、审查起诉阶段、审判阶段、执行阶段
                2. 强制措施：取保候审、监视居住、刑事拘留、逮捕
                3. 涉嫌罪名：具体指控的罪名
                4. 涉案金额：涉及的金额（如适用）
                5. 对事实的态度：认罪认罚、部分认可、不认可
                6. 律师介入：是否已委托律师
                
                ## 常用法律法规
                - 《刑法》
                - 《刑事诉讼法》
                - 认罪认罚相关规定
                
                ## 重要期限规定
                - 传唤/拘传：一般12小时，特别重大复杂的24小时
                - 刑事拘留：一般3-7天，流窜/多次/结伙作案的可延长至30天
                - 审查逮捕：检察机关7天内决定
                - 侦查羁押期限：一般2个月，可延长
                - 审查起诉期限：一般1个月，可延长15日
                - 一审审理期限：一般2个月，至迟3个月
                
                ## 取保候审条件
                1. 可能判处管制、拘役或者独立适用附加刑的
                2. 可能判处有期徒刑以上刑罚，采取取保候审不致发生社会危险性的
                3. 患有严重疾病、生活不能自理，怀孕或者正在哺乳自己婴儿的妇女
                4. 羁押期限届满，案件尚未办结
                
                ## 典型案例要点
                - 积极退赔、取得被害人谅解有助于取保候审
                - 认罪认罚可以从宽处理
                - 血液酒精含量80mg/100ml以上构成醉驾
                - 160mg/100ml以下且无其他从重情节的，可能不起诉或缓刑
                - 故意伤害致人轻伤以上构成犯罪
                """;
    }

    @Override
    public QuestionFormDTO execute(DialogueContext context, Object... args) {
        log.info("执行刑事诉讼Skill, sessionId: {}", context.getSessionId());

        askUserQuestionTool.setCurrentContext(context);
        AskUserQuestionTool.QuestionFormResult result = askUserQuestionTool.askUserQuestion("criminal_procedure", "");

        if (result.getSuccess() && result.getQuestionForm() != null) {
            return result.getQuestionForm();
        }

        throw new RuntimeException("生成问题表单失败: " + result.getErrorMessage());
    }
}
