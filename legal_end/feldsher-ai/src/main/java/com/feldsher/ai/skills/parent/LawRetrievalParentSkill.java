package com.feldsher.ai.skills.parent;

import com.feldsher.ai.skills.LegalSkill;
import com.feldsher.ai.tools.LawSearchTool;
import com.feldsher.common.context.DialogueContext;
import com.feldsher.common.vo.LawRetrievalVO;
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
public class LawRetrievalParentSkill implements LegalSkill {

    private final LawSearchTool lawSearchTool;

    @Override
    public String getSkillCode() {
        return "law_retrieval";
    }

    @Override
    public String getSkillName() {
        return "法律法规检索Skill";
    }

    @Override
    public String getSkillType() {
        return "parent";
    }

    @Override
    public String getPromptTemplate() {
        try {
            ClassPathResource resource = new ClassPathResource("prompts/skills/parent/law_retrieval_skill.md");
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
            log.error("读取法律法规检索Skill提示词失败", e);
            return getDefaultPrompt();
        }
    }

    private String getDefaultPrompt() {
        return """
                # 法律法规检索Skill
                
                ## 角色定义
                你是一个专业的法律法规检索专家。你的核心职责是根据案情事实，识别相关的法律问题点，检索适用的法律法规、司法解释，整理法条内容并分析与本案的关联性。
                
                ## 检索范围
                1. 法律：全国人大及其常委会制定的法律
                2. 司法解释：最高人民法院、最高人民检察院的解释
                3. 行政法规：国务院制定的条例、规定
                
                ## 检索原则
                - 针对性：只检索与本案直接相关的法条
                - 时效性：优先适用最新生效的法律规定
                - 效力层级：上位法优于下位法
                
                ## 输出要求
                每个检索结果需要包含：
                1. 法律名称和条文编号
                2. 法条原文内容
                3. 与本案的关联性分析
                4. 核心要点提炼
                """;
    }

    @Override
    public List<LawRetrievalVO> execute(DialogueContext context, Object... args) {
        log.info("执行法律法规检索Skill, sessionId: {}", context.getSessionId());

        String caseTypeCode = context.getCaseTypeCode();
        if (caseTypeCode == null) {
            caseTypeCode = "other";
        }

        String caseFacts = "";
        if (context.getCaseFacts() != null) {
            caseFacts = context.getCaseFacts().getSummary();
            if (caseFacts == null) {
                caseFacts = context.getCaseFacts().getSupplement();
            }
        }

        LawSearchTool.LawSearchResult result = lawSearchTool.searchLaws(caseTypeCode, caseFacts, "[]");

        if (result.getSuccess() && result.getResults() != null) {
            return result.getResults();
        }

        throw new RuntimeException("法律法规检索失败: " + result.getErrorMessage());
    }
}
