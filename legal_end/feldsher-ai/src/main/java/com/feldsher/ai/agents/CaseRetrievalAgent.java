package com.feldsher.ai.agents;

import com.feldsher.ai.skills.parent.CaseRetrievalParentSkill;
import com.feldsher.common.context.DialogueContext;
import com.feldsher.common.vo.CaseRetrievalVO;
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
public class CaseRetrievalAgent implements LegalAgent {

    private final CaseRetrievalParentSkill caseRetrievalParentSkill;

    @Override
    public String getAgentCode() {
        return "case_retrieval";
    }

    @Override
    public String getAgentName() {
        return "案例检索Agent";
    }

    @Override
    public String getPromptTemplate() {
        try {
            ClassPathResource resource = new ClassPathResource("prompts/agents/case_retrieval_agent.md");
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
            log.error("读取案例检索Agent提示词失败", e);
            return getDefaultPrompt();
        }
    }

    private String getDefaultPrompt() {
        return """
                # 案例检索Agent
                
                ## 角色定义
                你是一个专业的案例检索Agent。你的核心职责是：
                1. 根据案情事实和争议焦点，检索相似的司法案例
                2. 分析案例的裁判要点和参考价值
                
                ## 检索范围
                1. 最高人民法院指导案例
                2. 最高人民法院典型案例
                3. 地方各级法院案例
                4. 本院及上级法院案例
                
                ## 检索维度
                - 案由相同：同一法律关系类型
                - 事实相似：关键事实要素重合
                - 争议焦点相同：当事人争议的法律问题相同
                - 裁判法院相近：同一地区或同一层级法院
                - 裁判时间相近：近年的案例更有参考价值
                
                ## 相似度评估
                - 事实相似度：40%权重
                - 争议焦点相似度：30%权重
                - 法院层级：15%权重
                - 裁判时间：15%权重
                
                ## 输出要求
                每个案例需要包含：
                1. 案例基本信息（案号、法院、裁判日期）
                2. 基本案情
                3. 争议焦点
                4. 法院认定
                5. 裁判结果
                6. 关联性分析
                7. 核心要点提炼
                """;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<CaseRetrievalVO> execute(DialogueContext context, Object... args) {
        log.info("案例检索Agent开始执行, sessionId: {}", context.getSessionId());

        Object result = caseRetrievalParentSkill.execute(context, args);
        if (result instanceof List<?> list) {
            return (List<CaseRetrievalVO>) list;
        }

        throw new RuntimeException("案例检索失败");
    }
}
