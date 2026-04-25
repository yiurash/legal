package com.feldsher.ai.tools;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.feldsher.ai.service.PromptProvider;
import com.feldsher.ai.service.RealTimeStreamingService;
import com.feldsher.common.context.DialogueContext;
import com.feldsher.common.dto.QuestionFormDTO;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AskUserQuestionTool {

    private static final String PROMPT_KEY = "tools/other_question_generation";

    private final RealTimeStreamingService realTimeStreamingService;
    private final PromptProvider promptProvider;

    private DialogueContext currentContext;

    public void setCurrentContext(DialogueContext context) {
        this.currentContext = context;
    }

    @Tool("根据案由类型，生成需要向用户询问的问题表单，每个问题有2-4个选项，最后一个选项是暂不选择")
    public QuestionFormResult askUserQuestion(
            @P("案由类型代码，如marriage_family、labor_dispute等") String caseTypeCode,
            @P("用户已经提供的初步信息") String existingInfo) {
        
        log.info("开始生成问题表单，案由类型: {}", caseTypeCode);

        QuestionFormResult result = QuestionFormResult.builder()
                .success(true)
                .build();

        try {
            QuestionFormDTO questionForm = generateQuestionsByCaseType(caseTypeCode, existingInfo);
            result.setQuestionForm(questionForm);

            if (currentContext != null) {
                currentContext.setPendingQuestions(questionForm);
            }

            log.info("问题表单生成成功: {}", JSON.toJSONString(questionForm));
        } catch (Exception e) {
            log.error("生成问题表单失败", e);
            result.setSuccess(false);
            result.setErrorMessage("生成问题表单失败: " + e.getMessage());
        }

        return result;
    }

    private QuestionFormDTO generateQuestionsByCaseType(String caseTypeCode, String existingInfo) {
        List<QuestionFormDTO.QuestionItem> questions = new ArrayList<>();

        switch (caseTypeCode) {
            case "marriage_family":
                questions = generateMarriageFamilyQuestions();
                break;
            case "labor_dispute":
                questions = generateLaborDisputeQuestions();
                break;
            case "criminal_procedure":
                questions = generateCriminalProcedureQuestions();
                break;
            case "contract_dispute":
                questions = generateContractDisputeQuestions();
                break;
            default:
                questions = generateGeneralQuestionsByLlm(caseTypeCode, existingInfo);
        }

        return QuestionFormDTO.builder()
                .caseType(getCaseTypeName(caseTypeCode))
                .caseTypeCode(caseTypeCode)
                .questions(questions)
                .hasSupplement(true)
                .supplementHint("请补充说明其他相关事实...")
                .build();
    }

    private List<QuestionFormDTO.QuestionItem> generateGeneralQuestionsByLlm(String caseTypeCode, String existingInfo) {
        String systemPrompt = promptProvider.getPrompt(PROMPT_KEY, defaultPrompt());

        String userPrompt = "caseTypeCode=" + caseTypeCode + "\nexistingInfo=" + (existingInfo == null ? "" : existingInfo);
        long start = System.currentTimeMillis();
        RealTimeStreamingService.StreamingResult llmResult = realTimeStreamingService.chatSync(systemPrompt, userPrompt);
        long duration = System.currentTimeMillis() - start;
        int outputLen = llmResult != null && llmResult.getResponseContent() != null ? llmResult.getResponseContent().length() : 0;
        log.info("LLM问题生成完成。prompt_key={}, duration_ms={}, output_len={}", PROMPT_KEY, duration, outputLen);
        if (llmResult == null || !llmResult.isSuccess()) {
            log.warn("LLM问题生成失败，回退到通用模板。prompt_key={}", PROMPT_KEY);
            return generateGeneralQuestions();
        }

        try {
            String jsonText = extractJson(llmResult.getResponseContent());
            if (jsonText == null) {
                return generateGeneralQuestions();
            }
            JSONObject root = JSON.parseObject(jsonText);
            JSONArray array = root.getJSONArray("questions");
            if (array == null || array.isEmpty()) {
                return generateGeneralQuestions();
            }

            List<QuestionFormDTO.QuestionItem> questions = new ArrayList<>();
            for (int i = 0; i < array.size(); i++) {
                JSONObject q = array.getJSONObject(i);
                String questionText = q.getString("question");
                JSONArray optionsArray = q.getJSONArray("options");
                if (questionText == null || questionText.isBlank() || optionsArray == null || optionsArray.isEmpty()) {
                    continue;
                }

                List<String> options = optionsArray.toJavaList(String.class);
                if (options.isEmpty()) {
                    continue;
                }
                if (!"暂不选择".equals(options.get(options.size() - 1))) {
                    options.add("暂不选择");
                }

                questions.add(QuestionFormDTO.QuestionItem.builder()
                        .id("q" + (i + 1))
                        .question(questionText)
                        .options(options)
                        .order(i + 1)
                        .followUp(q.getString("followUp"))
                        .build());
            }

            return questions.isEmpty() ? generateGeneralQuestions() : questions;
        } catch (Exception e) {
            log.warn("LLM动态问题生成失败，回退到通用模板: {}", e.getMessage());
            return generateGeneralQuestions();
        }
    }

    private String defaultPrompt() {
        return """
返回JSON: {"questions":[{"id":"q1","question":"请补充关键事实","options":["暂不选择"]}]}。
""";
    }

    private List<QuestionFormDTO.QuestionItem> generateMarriageFamilyQuestions() {
        List<QuestionFormDTO.QuestionItem> questions = new ArrayList<>();

        questions.add(QuestionFormDTO.QuestionItem.builder()
                .id("q1")
                .question("您与对方目前的婚姻状态是？")
                .options(List.of("已婚且共同居住", "已婚但已分居", "已办理离婚登记", "暂不选择"))
                .order(1)
                .followUp("如果已分居，请问分居多长时间了？")
                .build());

        questions.add(QuestionFormDTO.QuestionItem.builder()
                .id("q2")
                .question("是否有未成年子女需要抚养？")
                .options(List.of("有，1个子女", "有，2个及以上子女", "没有子女", "暂不选择"))
                .order(2)
                .followUp("请说明子女的年龄和目前的生活状况：")
                .build());

        questions.add(QuestionFormDTO.QuestionItem.builder()
                .id("q3")
                .question("夫妻共同财产主要包括哪些？")
                .options(List.of("有房产需要分割", "有存款/投资需要分割", "没有共同财产", "暂不选择"))
                .order(3)
                .followUp("请简要说明财产的具体情况：")
                .build());

        questions.add(QuestionFormDTO.QuestionItem.builder()
                .id("q4")
                .question("您想要离婚的主要原因是什么？")
                .options(List.of("感情不和，经常吵架", "对方有出轨/婚外情", "对方有家暴/虐待行为", "暂不选择"))
                .order(4)
                .followUp("请具体说明情况：")
                .build());

        questions.add(QuestionFormDTO.QuestionItem.builder()
                .id("q5")
                .question("目前是否已经进入诉讼程序？")
                .options(List.of("还没有起诉，正在考虑", "已经起诉，正在审理中", "已经起诉过，法院判决不准离婚", "暂不选择"))
                .order(5)
                .followUp("请说明具体的时间节点：")
                .build());

        return questions;
    }

    private List<QuestionFormDTO.QuestionItem> generateLaborDisputeQuestions() {
        List<QuestionFormDTO.QuestionItem> questions = new ArrayList<>();

        questions.add(QuestionFormDTO.QuestionItem.builder()
                .id("q1")
                .question("您与单位之间是什么用工关系？")
                .options(List.of("签订了书面劳动合同", "没有签订书面劳动合同", "劳务派遣用工", "暂不选择"))
                .order(1)
                .followUp("请说明入职时间和工作岗位：")
                .build());

        questions.add(QuestionFormDTO.QuestionItem.builder()
                .id("q2")
                .question("您遇到的主要问题是什么？")
                .options(List.of("单位拖欠工资/加班工资", "单位违法解除劳动合同", "单位没有缴纳社保", "暂不选择"))
                .order(2)
                .followUp("请具体说明情况：")
                .build());

        questions.add(QuestionFormDTO.QuestionItem.builder()
                .id("q3")
                .question("您的月工资标准是多少？")
                .options(List.of("3000元以下", "3000-10000元", "10000元以上", "暂不选择"))
                .order(3)
                .followUp("请说明工资构成：")
                .build());

        questions.add(QuestionFormDTO.QuestionItem.builder()
                .id("q4")
                .question("您目前掌握哪些证据材料？")
                .options(List.of("有劳动合同", "有工资发放记录", "有工作证/工牌/考勤记录", "暂不选择"))
                .order(4)
                .followUp("请具体说明您掌握的证据：")
                .build());

        questions.add(QuestionFormDTO.QuestionItem.builder()
                .id("q5")
                .question("目前您已经采取了哪些维权措施？")
                .options(List.of("还没有采取任何措施", "已经与单位协商过", "已经申请劳动仲裁", "暂不选择"))
                .order(5)
                .followUp("请说明具体的时间节点和结果：")
                .build());

        return questions;
    }

    private List<QuestionFormDTO.QuestionItem> generateCriminalProcedureQuestions() {
        List<QuestionFormDTO.QuestionItem> questions = new ArrayList<>();

        questions.add(QuestionFormDTO.QuestionItem.builder()
                .id("q1")
                .question("目前案件处于什么阶段？")
                .options(List.of("只是被询问/调查，还未立案", "公安机关侦查阶段", "检察机关审查起诉阶段", "暂不选择"))
                .order(1)
                .followUp("请说明具体的时间节点：")
                .build());

        questions.add(QuestionFormDTO.QuestionItem.builder()
                .id("q2")
                .question("目前是否被采取了强制措施？")
                .options(List.of("没有被采取任何强制措施", "取保候审", "刑事拘留/逮捕", "暂不选择"))
                .order(2)
                .followUp("如果被采取强制措施，请说明时间和执行机关：")
                .build());

        questions.add(QuestionFormDTO.QuestionItem.builder()
                .id("q3")
                .question("公安机关/检察机关指控的罪名是什么？")
                .options(List.of("诈骗罪/合同诈骗罪", "故意伤害罪", "危险驾驶罪（醉驾）", "暂不选择"))
                .order(3)
                .followUp("请说明具体的指控内容：")
                .build());

        questions.add(QuestionFormDTO.QuestionItem.builder()
                .id("q4")
                .question("对于指控的事实，您的态度是？")
                .options(List.of("事实属实，愿意认罪认罚", "部分事实属实，但定性有问题", "事实不属实，是被冤枉的", "暂不选择"))
                .order(4)
                .followUp("请详细说明您认为的事实情况：")
                .build());

        questions.add(QuestionFormDTO.QuestionItem.builder()
                .id("q5")
                .question("目前是否已经委托律师？")
                .options(List.of("已经委托律师，律师正在处理", "还没有委托律师，正在考虑", "申请了法律援助", "暂不选择"))
                .order(5)
                .followUp("请说明律师已做的工作：")
                .build());

        return questions;
    }

    private List<QuestionFormDTO.QuestionItem> generateContractDisputeQuestions() {
        List<QuestionFormDTO.QuestionItem> questions = new ArrayList<>();

        questions.add(QuestionFormDTO.QuestionItem.builder()
                .id("q1")
                .question("您涉及的是什么类型的合同？")
                .options(List.of("买卖合同（货物买卖）", "借款合同（民间借贷）", "租赁合同", "暂不选择"))
                .order(1)
                .followUp("请说明合同的主要内容：")
                .build());

        questions.add(QuestionFormDTO.QuestionItem.builder()
                .id("q2")
                .question("合同是如何订立的？")
                .options(List.of("签订了书面合同", "有口头约定，没有书面合同", "通过微信/邮件等方式达成协议", "暂不选择"))
                .order(2)
                .followUp("请说明合同签订的时间和各方主体：")
                .build());

        questions.add(QuestionFormDTO.QuestionItem.builder()
                .id("q3")
                .question("您遇到的主要问题是什么？")
                .options(List.of("对方不履行合同义务", "对方迟延履行合同义务", "对方履行不符合合同约定", "暂不选择"))
                .order(3)
                .followUp("请具体说明争议的情况：")
                .build());

        questions.add(QuestionFormDTO.QuestionItem.builder()
                .id("q4")
                .question("合同涉及的金额大概是多少？")
                .options(List.of("10万元以下", "10万-100万元", "100万元以上", "暂不选择"))
                .order(4)
                .followUp("请说明金额的具体构成：")
                .build());

        questions.add(QuestionFormDTO.QuestionItem.builder()
                .id("q5")
                .question("您目前掌握哪些证据材料？")
                .options(List.of("有书面合同/协议", "有付款/收款凭证", "有沟通记录（微信/邮件/短信）", "暂不选择"))
                .order(5)
                .followUp("请具体说明您掌握的证据：")
                .build());

        return questions;
    }

    private List<QuestionFormDTO.QuestionItem> generateGeneralQuestions() {
        List<QuestionFormDTO.QuestionItem> questions = new ArrayList<>();

        questions.add(QuestionFormDTO.QuestionItem.builder()
                .id("q1")
                .question("【事件】是什么时候发生的？")
                .options(List.of("1个月以内", "1-6个月", "6个月以上", "暂不选择"))
                .order(1)
                .followUp("具体时间是：")
                .build());

        questions.add(QuestionFormDTO.QuestionItem.builder()
                .id("q2")
                .question("涉及的当事人有哪些？")
                .options(List.of("仅您本人", "您和对方两个人", "多方参与（3人及以上）", "暂不选择"))
                .order(2)
                .followUp("请简要说明各方关系：")
                .build());

        questions.add(QuestionFormDTO.QuestionItem.builder()
                .id("q3")
                .question("您目前掌握哪些证据材料？")
                .options(List.of("有书面合同/协议", "有聊天记录/通话录音", "有转账记录/支付凭证", "暂不选择"))
                .order(3)
                .followUp("请具体说明证据情况：")
                .build());

        questions.add(QuestionFormDTO.QuestionItem.builder()
                .id("q4")
                .question("目前事情处于什么阶段？")
                .options(List.of("刚发生，还未采取任何行动", "已与对方协商过", "已向有关部门反映/投诉", "暂不选择"))
                .order(4)
                .followUp("请说明具体进展情况：")
                .build());

        return questions;
    }

    private String getCaseTypeName(String caseTypeCode) {
        return switch (caseTypeCode) {
            case "marriage_family" -> "婚姻家事";
            case "labor_dispute" -> "劳务纠纷";
            case "criminal_procedure" -> "刑事诉讼";
            case "contract_dispute" -> "合同纠纷";
            default -> "其他";
        };
    }

    private String extractJson(String text) {
        if (text == null) {
            return null;
        }
        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return null;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionFormResult implements Serializable {
        private static final long serialVersionUID = 1L;

        private Boolean success;
        private QuestionFormDTO questionForm;
        private String errorMessage;
    }
}
