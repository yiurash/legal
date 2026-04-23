package com.feldsher.ai.agents;

import com.feldsher.ai.skills.parent.LawRetrievalParentSkill;
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
public class LawRetrievalAgent implements LegalAgent {

    private final LawRetrievalParentSkill lawRetrievalParentSkill;

    @Override
    public String getAgentCode() {
        return "law_retrieval";
    }

    @Override
    public String getAgentName() {
        return "法律法规检索Agent";
    }

    @Override
    public String getPromptTemplate() {
        try {
            ClassPathResource resource = new ClassPathResource("prompts/agents/law_retrieval_agent.md");
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
            log.error("读取法律法规检索Agent提示词失败", e);
            return getDefaultPrompt();
        }
    }

    private String getDefaultPrompt() {
        return """
                # 法律法规检索Agent
                
                ## 角色定义
                你是一个专业的法律法规检索Agent。你的核心职责是：
                1. 根据案情事实，识别相关的法律问题点
                2. 检索适用的法律法规、司法解释
                3. 整理法条内容，分析与本案的关联性
                
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
    @SuppressWarnings("unchecked")
    public List<LawRetrievalVO> execute(DialogueContext context, Object... args) {
        log.info("法律法规检索Agent开始执行, sessionId: {}", context.getSessionId());

        Object result = lawRetrievalParentSkill.execute(context, args);
        if (result instanceof List<?> list) {
            return (List<LawRetrievalVO>) list;
        }

        throw new RuntimeException("法律法规检索失败");
    }
}
