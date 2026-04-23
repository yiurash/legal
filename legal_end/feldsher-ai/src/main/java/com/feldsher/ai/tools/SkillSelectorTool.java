package com.feldsher.ai.tools;

import com.feldsher.common.context.DialogueContext;
import com.feldsher.common.enums.CaseTypeEnum;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class SkillSelectorTool {

    private DialogueContext currentContext;

    public void setCurrentContext(DialogueContext context) {
        this.currentContext = context;
    }

    @Tool("根据用户描述的法律问题，分析并选择最合适的案由类型和对应的Skill")
    public SkillSelectionResult selectSkill(
            @P("用户描述的法律问题内容") String userQuestion,
            @P("当前已有的对话上下文信息，可以为空") String contextInfo) {
        
        log.info("开始分析用户问题，选择合适的Skill: {}", userQuestion);

        SkillSelectionResult result = SkillSelectionResult.builder()
                .success(true)
                .possibleSkills(new ArrayList<>())
                .build();

        String lowerQuestion = userQuestion.toLowerCase();

        if (lowerQuestion.contains("离婚") || lowerQuestion.contains("婚姻") || lowerQuestion.contains("财产分割")
                || lowerQuestion.contains("子女抚养") || lowerQuestion.contains("抚养权") || lowerQuestion.contains("抚养费")
                || lowerQuestion.contains("夫妻") || lowerQuestion.contains("结婚") || lowerQuestion.contains("彩礼")
                || lowerQuestion.contains("继承") || lowerQuestion.contains("遗产")) {
            result.addPossibleSkill(PossibleSkill.builder()
                    .caseType("婚姻家事")
                    .caseTypeCode("marriage_family")
                    .skillName("婚姻家事Skill")
                    .skillCode("marriage_family")
                    .confidence(0.9)
                    .reason("用户问题涉及婚姻、家庭、继承等相关法律问题")
                    .build());
        }

        if (lowerQuestion.contains("工资") || lowerQuestion.contains("拖欠") || lowerQuestion.contains("加班")
                || lowerQuestion.contains("劳动合同") || lowerQuestion.contains("辞退") || lowerQuestion.contains("解雇")
                || lowerQuestion.contains("社保") || lowerQuestion.contains("工伤") || lowerQuestion.contains("劳动")
                || lowerQuestion.contains("劳务") || lowerQuestion.contains("仲裁") || lowerQuestion.contains("经济补偿金")) {
            result.addPossibleSkill(PossibleSkill.builder()
                    .caseType("劳务纠纷")
                    .caseTypeCode("labor_dispute")
                    .skillName("劳务纠纷Skill")
                    .skillCode("labor_dispute")
                    .confidence(0.85)
                    .reason("用户问题涉及劳动用工、工资、工伤等劳动争议问题")
                    .build());
        }

        if (lowerQuestion.contains("诈骗") || lowerQuestion.contains("被骗") || lowerQuestion.contains("骗了")
                || lowerQuestion.contains("骗钱") || lowerQuestion.contains("被骗了") || lowerQuestion.contains("被骗走")
                || lowerQuestion.contains("盗窃") || lowerQuestion.contains("偷") || lowerQuestion.contains("被盗")
                || lowerQuestion.contains("抢劫") || lowerQuestion.contains("抢夺") || lowerQuestion.contains("拦路")
                || lowerQuestion.contains("故意伤害") || lowerQuestion.contains("打人") || lowerQuestion.contains("被打")
                || lowerQuestion.contains("刑事拘留") || lowerQuestion.contains("取保候审")
                || lowerQuestion.contains("逮捕") || lowerQuestion.contains("判刑") || lowerQuestion.contains("坐牢")
                || lowerQuestion.contains("刑事") || lowerQuestion.contains("犯罪") || lowerQuestion.contains("犯法")
                || lowerQuestion.contains("醉驾") || lowerQuestion.contains("危险驾驶") || lowerQuestion.contains("酒驾")
                || lowerQuestion.contains("寻衅滋事") || lowerQuestion.contains("打架斗殴")
                || lowerQuestion.contains("传销") || lowerQuestion.contains("非法集资")
                || lowerQuestion.contains("敲诈") || lowerQuestion.contains("勒索")
                || lowerQuestion.contains("非法拘禁") || lowerQuestion.contains("绑架")
                || lowerQuestion.contains("贩毒") || lowerQuestion.contains("吸毒")
                || lowerQuestion.contains("强奸") || lowerQuestion.contains("猥亵")
                || lowerQuestion.contains("职务侵占") || lowerQuestion.contains("挪用公款")
                || lowerQuestion.contains("受贿") || lowerQuestion.contains("行贿")) {
            result.addPossibleSkill(PossibleSkill.builder()
                    .caseType("刑事诉讼")
                    .caseTypeCode("criminal_procedure")
                    .skillName("刑事诉讼Skill")
                    .skillCode("criminal_procedure")
                    .confidence(0.9)
                    .reason("用户问题涉及刑事犯罪、刑事拘留、刑事诉讼等相关问题")
                    .build());
        }

        if (lowerQuestion.contains("合同") || lowerQuestion.contains("违约") || lowerQuestion.contains("借款")
                || lowerQuestion.contains("欠钱") || lowerQuestion.contains("租赁") || lowerQuestion.contains("买卖")
                || lowerQuestion.contains("建设工程") || lowerQuestion.contains("工程款") || lowerQuestion.contains("服务")
                || lowerQuestion.contains("合伙") || lowerQuestion.contains("合作") || lowerQuestion.contains("解除合同")) {
            result.addPossibleSkill(PossibleSkill.builder()
                    .caseType("合同纠纷")
                    .caseTypeCode("contract_dispute")
                    .skillName("合同纠纷Skill")
                    .skillCode("contract_dispute")
                    .confidence(0.8)
                    .reason("用户问题涉及合同订立、履行、违约责任等合同相关问题")
                    .build());
        }

        if (result.getPossibleSkills().isEmpty()) {
            if (isMeaninglessInput(userQuestion)) {
                result.setSuccess(false);
                result.setErrorMessage("NON_LEGAL_QUESTION");
                log.warn("检测到无意义输入或非法律问题: {}", userQuestion);
                return result;
            }
            
            result.addPossibleSkill(PossibleSkill.builder()
                    .caseType("其他")
                    .caseTypeCode("other")
                    .skillName("通用法律Skill")
                    .skillCode("general")
                    .confidence(0.5)
                    .reason("无法识别具体案由，使用通用法律Skill")
                    .build());
        }

        if (currentContext != null && !result.getPossibleSkills().isEmpty()) {
            PossibleSkill topSkill = result.getPossibleSkills().get(0);
            currentContext.setCaseType(topSkill.getCaseType());
            currentContext.setCaseTypeCode(topSkill.getCaseTypeCode());
            currentContext.setCurrentSkill(topSkill.getSkillCode());
        }

        log.info("Skill选择结果: {}", result);
        return result;
    }

    private boolean isMeaninglessInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return true;
        }
        
        String trimmed = input.trim();
        
        if (trimmed.matches("^\\d+$")) {
            return true;
        }
        
        if (trimmed.matches("^[a-zA-Z]+$")) {
            return true;
        }
        
        if (trimmed.matches("^[\\s\\p{Punct}]+$")) {
            return true;
        }
        
        if (trimmed.length() < 2) {
            return true;
        }
        
        if (trimmed.length() <= 5 && !hasLegalKeywords(trimmed)) {
            return true;
        }
        
        return false;
    }

    private boolean hasLegalKeywords(String input) {
        String lower = input.toLowerCase();
        return lower.contains("法律") || lower.contains("法") || lower.contains("律")
                || lower.contains("律师") || lower.contains("法院") || lower.contains("诉讼")
                || lower.contains("合同") || lower.contains("离婚") || lower.contains("劳动")
                || lower.contains("纠纷") || lower.contains("赔偿") || lower.contains("欠款")
                || lower.contains("钱") || lower.contains("债") || lower.contains("婚")
                || lower.contains("伤") || lower.contains("罪") || lower.contains("骗");
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillSelectionResult implements Serializable {
        private static final long serialVersionUID = 1L;

        private Boolean success;
        private List<PossibleSkill> possibleSkills;
        private String errorMessage;

        public void addPossibleSkill(PossibleSkill skill) {
            if (this.possibleSkills == null) {
                this.possibleSkills = new ArrayList<>();
            }
            this.possibleSkills.add(skill);
            this.possibleSkills.sort((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()));
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PossibleSkill implements Serializable {
        private static final long serialVersionUID = 1L;

        private String caseType;
        private String caseTypeCode;
        private String skillName;
        private String skillCode;
        private Double confidence;
        private String reason;
    }
}
