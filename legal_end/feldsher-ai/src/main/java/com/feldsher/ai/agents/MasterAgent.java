package com.feldsher.ai.agents;

import com.alibaba.fastjson2.JSON;
import com.feldsher.ai.skills.LegalSkill;
import com.feldsher.ai.skills.SkillRegistry;
import com.feldsher.ai.tools.SkillSelectorTool;
import com.feldsher.common.context.DialogueContext;
import com.feldsher.common.dto.IntentionResultDTO;
import com.feldsher.common.dto.QuestionFormDTO;
import com.feldsher.common.enums.DialogueStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MasterAgent implements LegalAgent {

    private final SkillSelectorTool skillSelectorTool;
    private final SkillRegistry skillRegistry;
    private final FactCollectionAgent factCollectionAgent;
    private final LawRetrievalAgent lawRetrievalAgent;
    private final CaseRetrievalAgent caseRetrievalAgent;

    @Override
    public String getAgentCode() {
        return "master";
    }

    @Override
    public String getAgentName() {
        return "主Agent";
    }

    @Override
    public String getPromptTemplate() {
        try {
            ClassPathResource resource = new ClassPathResource("prompts/agents/master_agent.md");
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
            log.error("读取主Agent提示词失败", e);
            return getDefaultPrompt();
        }
    }

    private String getDefaultPrompt() {
        return """
                # 主Agent
                
                ## 角色定义
                你是一个专业的法律助手AI系统的主控Agent。你的核心职责是：
                1. 意图识别：准确理解用户的法律问题，判断用户的真实需求
                2. 路由分配：根据用户意图，将任务分发给合适的子Agent处理
                3. 结果汇总：汇总各子Agent返回的结果，形成完整的法律建议
                
                ## 工作流程
                1. 分析用户输入，识别意图和案由类型
                2. 选择合适的Skill
                3. 决定是否需要采集更多信息
                4. 路由到对应的子Agent处理
                5. 汇总所有结果，形成最终结论
                """;
    }

    @Override
    public Object execute(DialogueContext context, Object... args) {
        log.info("主Agent开始执行, sessionId: {}, 状态: {}", context.getSessionId(), context.getCurrentStatus());

        String userInput = (String) args[0];

        String currentStatus = context.getCurrentStatus();
        if (currentStatus == null || DialogueStatusEnum.INIT.getCode().equals(currentStatus)) {
            return handleInitialInput(context, userInput);
        } else if (DialogueStatusEnum.QUESTION_COLLECTING.getCode().equals(currentStatus)) {
            return handleQuestionCollection(context, userInput);
        } else if (DialogueStatusEnum.CONCLUDING.getCode().equals(currentStatus)) {
            return handleConclusion(context, userInput);
        }

        return handleDefault(context, userInput);
    }

    private MasterAgentResult handleInitialInput(DialogueContext context, String userInput) {
        log.info("处理初始用户输入: {}", userInput);

        SkillSelectorTool.SkillSelectionResult skillResult = skillSelectorTool.selectSkill(userInput, "");
        log.info("Skill选择结果: {}", JSON.toJSONString(skillResult));

        if (!skillResult.getSuccess()) {
            if ("NON_LEGAL_QUESTION".equals(skillResult.getErrorMessage())) {
                return MasterAgentResult.builder()
                        .success(true)
                        .isNonLegalQuestion(true)
                        .thinkingContent("亲，我只能回答法律方面问题哦~")
                        .build();
            }
            return MasterAgentResult.builder()
                    .success(false)
                    .errorMessage("无法识别您的法律问题，请提供更多信息")
                    .build();
        }

        SkillSelectorTool.PossibleSkill topSkill = skillResult.getPossibleSkills().get(0);
        log.info("选择最匹配的Skill: {} - {}", topSkill.getSkillName(), topSkill.getCaseType());

        context.setCaseType(topSkill.getCaseType());
        context.setCaseTypeCode(topSkill.getCaseTypeCode());
        context.setCurrentSkill(topSkill.getSkillCode());
        context.setCurrentAgent("fact_collection");
        context.setCurrentStatus(DialogueStatusEnum.INTENTION_ANALYZING.getCode());

        IntentionResultDTO intention = IntentionResultDTO.builder()
                .intention("综合咨询")
                .caseType(topSkill.getCaseType())
                .caseTypeCode(topSkill.getCaseTypeCode())
                .needMoreInfo(true)
                .confidence(topSkill.getConfidence())
                .nextAgent("fact_collection")
                .nextSkill(topSkill.getSkillCode())
                .build();

        context.setCurrentStatus(DialogueStatusEnum.QUESTION_COLLECTING.getCode());

        QuestionFormDTO questionForm = (QuestionFormDTO) factCollectionAgent.execute(context, userInput);
        log.info("生成问题表单: {}", JSON.toJSONString(questionForm));

        return MasterAgentResult.builder()
                .success(true)
                .intention(intention)
                .questionForm(questionForm)
                .nextStep("question_collection")
                .thinkingContent("我正在分析您的法律问题...根据您的描述，这看起来是一个" + topSkill.getCaseType() + "类案件。为了给您提供更准确的法律建议，我需要了解一些具体信息。")
                .build();
    }

    @SuppressWarnings("unchecked")
    private MasterAgentResult handleQuestionCollection(DialogueContext context, String userInput) {
        log.info("处理问题收集阶段的用户输入");

        try {
            if (userInput.startsWith("{") && userInput.endsWith("}")) {
                var answerMap = JSON.parseObject(userInput);
                log.info("解析用户回答: {}", answerMap);

                String caseType = context.getCaseType();
                String caseTypeCode = context.getCaseTypeCode();

                var answers = answerMap.getJSONObject("answers");
                String supplement = answerMap.getString("supplement");

                StringBuilder factsBuilder = new StringBuilder();
                if (answers != null) {
                    for (String key : answers.keySet()) {
                        factsBuilder.append(key).append(": ").append(answers.getString(key)).append("; ");
                    }
                }
                if (supplement != null && !supplement.isEmpty()) {
                    factsBuilder.append("补充说明: ").append(supplement);
                }

                var caseFacts = new com.feldsher.common.dto.CaseFactsDTO();
                caseFacts.setCaseType(caseType);
                caseFacts.setCaseTypeCode(caseTypeCode);
                caseFacts.setSupplement(supplement);
                caseFacts.setSummary(factsBuilder.toString());
                caseFacts.setKeyPoints(List.of("用户已提供关键事实信息"));

                context.setCaseFacts(caseFacts);

                log.info("开始检索法律法规...");
                var lawResults = (List<com.feldsher.common.vo.LawRetrievalVO>) lawRetrievalAgent.execute(context);
                log.info("法律法规检索完成，找到 {} 条", lawResults.size());

                log.info("开始检索相似案例...");
                var caseResults = (List<com.feldsher.common.vo.CaseRetrievalVO>) caseRetrievalAgent.execute(context);
                log.info("案例检索完成，找到 {} 条", caseResults.size());

                context.setCurrentStatus(DialogueStatusEnum.CONCLUDING.getCode());

                String caseAnalysis = generateCaseAnalysis(context);

                var conclusion = com.feldsher.common.vo.DialogueResponseVO.FinalConclusion.builder()
                        .caseAnalysis(caseAnalysis)
                        .lawResults(lawResults)
                        .caseResults(caseResults)
                        .summary(generateSummary(context))
                        .nextSteps(generateNextSteps(context))
                        .build();

                return MasterAgentResult.builder()
                        .success(true)
                        .conclusion(conclusion)
                        .nextStep("completed")
                        .isComplete(true)
                        .thinkingContent("我正在根据您提供的信息进行法律分析...正在检索相关的法律法规和相似案例...")
                        .build();
            }
        } catch (Exception e) {
            log.error("处理用户回答失败", e);
        }

        return MasterAgentResult.builder()
                .success(false)
                .errorMessage("请提供有效的回答")
                .build();
    }

    private MasterAgentResult handleConclusion(DialogueContext context, String userInput) {
        log.info("处理结论阶段的用户输入: {}", userInput);
        
        if (userInput != null && !userInput.isEmpty() && !userInput.startsWith("{")) {
            log.info("检测到用户新的问题输入，重置状态重新开始意图识别");
            context.setCurrentStatus(DialogueStatusEnum.INIT.getCode());
            context.setCaseFacts(null);
            context.setCaseType(null);
            context.setCaseTypeCode(null);
            context.setCurrentSkill(null);
            context.setCurrentAgent(null);
            return handleInitialInput(context, userInput);
        }
        
        return MasterAgentResult.builder()
                .success(true)
                .nextStep("completed")
                .isComplete(true)
                .thinkingContent("您的咨询已经完成。如果还有其他问题，可以直接输入问题继续咨询，或输入 /new 开始新的对话。")
                .build();
    }

    private MasterAgentResult handleDefault(DialogueContext context, String userInput) {
        return handleInitialInput(context, userInput);
    }

    private String generateCaseAnalysis(DialogueContext context) {
        var caseFacts = context.getCaseFacts();
        if (caseFacts == null) {
            return "根据您描述的情况，我为您进行以下法律分析：";
        }

        return String.format("根据您提供的信息，这是一个%s类案件。\n\n" +
                "【案情概述】\n" +
                "%s\n\n" +
                "【法律分析要点】\n" +
                "1. 首先，需要确认案件的法律关系性质和适用的法律规范；\n" +
                "2. 其次，需要分析各方的权利义务和可能的法律后果；\n" +
                "3. 最后，根据相关法律法规和类似案例，给出初步的法律建议。",
                caseFacts.getCaseType(),
                caseFacts.getSummary() != null ? caseFacts.getSummary() : caseFacts.getSupplement());
    }

    private String generateSummary(DialogueContext context) {
        var caseFacts = context.getCaseFacts();
        return String.format("综合以上分析，对于您咨询的%s问题，建议您：\n\n" +
                "1. 首先收集和整理所有相关证据材料；\n" +
                "2. 可以先尝试与对方协商解决；\n" +
                "3. 如协商不成，可以考虑通过法律途径维护自身权益；\n" +
                "4. 建议咨询专业律师获取更详细的法律意见。",
                caseFacts != null ? caseFacts.getCaseType() : "法律");
    }

    private String generateNextSteps(DialogueContext context) {
        return """
                【下一步建议】
                
                1. **证据收集**：整理所有与案件相关的证据材料，包括合同、聊天记录、转账凭证、证人证言等。
                
                2. **法律咨询**：建议您携带相关证据材料，咨询专业律师获取详细的法律意见。
                
                3. **协商解决**：如果可能，可以尝试与对方进行协商，寻求和解方案。
                
                4. **法律途径**：如协商不成，可以考虑通过以下法律途径解决：
                   - 向相关部门投诉举报；
                   - 申请调解或仲裁；
                   - 向人民法院提起诉讼。
                
                5. **时效注意**：注意法律规定的诉讼时效，及时主张权利。
                
                【重要提示】
                以上分析仅供参考，不构成正式的法律意见。具体案件情况可能因证据、法律适用等因素而有所不同。建议您在采取任何法律行动前，咨询专业律师的意见。
                """;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class MasterAgentResult implements java.io.Serializable {
        private static final long serialVersionUID = 1L;

        private Boolean success;
        private IntentionResultDTO intention;
        private QuestionFormDTO questionForm;
        private com.feldsher.common.vo.DialogueResponseVO.FinalConclusion conclusion;
        private String nextStep;
        private String thinkingContent;
        private String errorMessage;
        private Boolean isComplete;
        private Boolean isNonLegalQuestion;
    }
}
