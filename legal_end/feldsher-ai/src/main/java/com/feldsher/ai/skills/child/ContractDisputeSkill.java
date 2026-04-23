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
public class ContractDisputeSkill implements LegalSkill {

    private final AskUserQuestionTool askUserQuestionTool;

    @Override
    public String getSkillCode() {
        return "contract_dispute";
    }

    @Override
    public String getSkillName() {
        return "合同纠纷Skill";
    }

    @Override
    public String getSkillType() {
        return "child";
    }

    @Override
    public String getPromptTemplate() {
        try {
            ClassPathResource resource = new ClassPathResource("prompts/skills/child/contract_dispute_skill.md");
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
            log.error("读取合同纠纷Skill提示词失败", e);
            return getDefaultPrompt();
        }
    }

    private String getDefaultPrompt() {
        return """
                # 合同纠纷Skill
                
                ## 适用案由
                - 买卖合同纠纷
                - 借款合同纠纷
                - 租赁合同纠纷
                - 承揽合同纠纷
                - 建设工程合同纠纷
                - 运输合同纠纷
                - 技术合同纠纷
                - 保管合同纠纷
                - 仓储合同纠纷
                - 委托合同纠纷
                - 物业服务合同纠纷
                - 合伙协议纠纷
                
                ## 核心问题
                1. 合同类型：买卖合同、借款合同、租赁合同、建设工程合同等
                2. 合同订立：书面合同、口头约定、微信/邮件协议
                3. 争议类型：不履行、迟延履行、履行不符合约定、解除合同
                4. 合同金额：涉及的金额
                5. 履行情况：各方履行的具体情况
                6. 证据情况：合同、付款凭证、沟通记录等
                
                ## 常用法律法规
                - 《民法典》合同编（第463-988条）
                - 最高人民法院相关司法解释
                
                ## 重要原则
                1. 依法成立的合同，对当事人具有法律约束力
                2. 当事人应当按照约定全面履行自己的义务
                3. 当事人一方不履行合同义务或者履行合同义务不符合约定的，应当承担违约责任
                
                ## 合同法定解除情形
                1. 因不可抗力致使不能实现合同目的
                2. 在履行期限届满前，当事人一方明确表示或者以自己的行为表明不履行主要债务
                3. 当事人一方迟延履行主要债务，经催告后在合理期限内仍未履行
                4. 当事人一方迟延履行债务或者有其他违约行为致使不能实现合同目的
                
                ## 诉讼时效
                - 普通诉讼时效：3年
                - 最长保护期：20年
                - 起算点：自权利人知道或者应当知道权利受到损害以及义务人之日起计算
                
                ## 典型案例要点
                - 民间借贷利率上限：合同成立时LPR的四倍
                - 超过上限的利息不予保护
                - 只有构成根本违约才能解除合同
                - 一般质量瑕疵不能解除合同，但可主张质量违约金
                - 利用中介公司提供的信息私下成交构成跳单
                - 跳单应当支付居间服务费
                """;
    }

    @Override
    public QuestionFormDTO execute(DialogueContext context, Object... args) {
        log.info("执行合同纠纷Skill, sessionId: {}", context.getSessionId());

        askUserQuestionTool.setCurrentContext(context);
        AskUserQuestionTool.QuestionFormResult result = askUserQuestionTool.askUserQuestion("contract_dispute", "");

        if (result.getSuccess() && result.getQuestionForm() != null) {
            return result.getQuestionForm();
        }

        throw new RuntimeException("生成问题表单失败: " + result.getErrorMessage());
    }
}
