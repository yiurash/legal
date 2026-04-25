package com.feldsher.ai.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.feldsher.common.context.DialogueContext;
import com.feldsher.common.vo.CaseRetrievalVO;
import com.feldsher.common.vo.DialogueResponseVO;
import com.feldsher.common.vo.LawRetrievalVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConclusionGenerationService {

    private static final String PROMPT_KEY = "tools/conclusion_generation";

    private final RealTimeStreamingService realTimeStreamingService;
    private final PromptProvider promptProvider;

    public DialogueResponseVO.FinalConclusion generateConclusion(
            DialogueContext context,
            List<LawRetrievalVO> lawResults,
            List<CaseRetrievalVO> caseResults
    ) {
        String fallbackCaseAnalysis = fallbackCaseAnalysis(context);
        String fallbackSummary = fallbackSummary(context);
        String fallbackNextSteps = fallbackNextSteps();

        String systemPrompt = promptProvider.getPrompt(PROMPT_KEY, defaultPrompt());
        String userPrompt = buildInput(context, lawResults, caseResults);
        long start = System.currentTimeMillis();
        RealTimeStreamingService.StreamingResult llmResult = realTimeStreamingService.chatSync(systemPrompt, userPrompt);
        long cost = System.currentTimeMillis() - start;
        log.info("结论生成执行完成。prompt_key={}, duration_ms={}", PROMPT_KEY, cost);

        if (llmResult == null || !llmResult.isSuccess()) {
            log.warn("结论LLM失败，使用fallback模板。prompt_key={}", PROMPT_KEY);
            return DialogueResponseVO.FinalConclusion.builder()
                    .caseAnalysis(fallbackCaseAnalysis)
                    .lawResults(lawResults)
                    .caseResults(caseResults)
                    .summary(fallbackSummary)
                    .nextSteps(fallbackNextSteps)
                    .build();
        }

        String jsonText = extractJson(llmResult.getResponseContent());
        if (jsonText == null) {
            log.warn("结论LLM未返回有效JSON，使用fallback模板。prompt_key={}", PROMPT_KEY);
            return DialogueResponseVO.FinalConclusion.builder()
                    .caseAnalysis(fallbackCaseAnalysis)
                    .lawResults(lawResults)
                    .caseResults(caseResults)
                    .summary(fallbackSummary)
                    .nextSteps(fallbackNextSteps)
                    .build();
        }

        try {
            JSONObject root = JSON.parseObject(jsonText);
            String caseAnalysis = root.getString("caseAnalysis");
            String summary = root.getString("summary");
            String nextSteps = root.getString("nextSteps");

            if (isBlank(caseAnalysis) || isBlank(summary) || isBlank(nextSteps)) {
                log.warn("结论LLM字段不完整，使用fallback模板。prompt_key={}", PROMPT_KEY);
                caseAnalysis = isBlank(caseAnalysis) ? fallbackCaseAnalysis : caseAnalysis;
                summary = isBlank(summary) ? fallbackSummary : summary;
                nextSteps = isBlank(nextSteps) ? fallbackNextSteps : nextSteps;
            }

            return DialogueResponseVO.FinalConclusion.builder()
                    .caseAnalysis(caseAnalysis)
                    .lawResults(lawResults)
                    .caseResults(caseResults)
                    .summary(summary)
                    .nextSteps(nextSteps)
                    .build();
        } catch (Exception e) {
            log.warn("结论LLM解析失败，使用fallback模板。prompt_key={}", PROMPT_KEY, e);
            return DialogueResponseVO.FinalConclusion.builder()
                    .caseAnalysis(fallbackCaseAnalysis)
                    .lawResults(lawResults)
                    .caseResults(caseResults)
                    .summary(fallbackSummary)
                    .nextSteps(fallbackNextSteps)
                    .build();
        }
    }

    private String buildInput(DialogueContext context, List<LawRetrievalVO> lawResults, List<CaseRetrievalVO> caseResults) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("caseType", context.getCaseType());
        payload.put("caseTypeCode", context.getCaseTypeCode());
        payload.put("caseFacts", context.getCaseFacts());
        payload.put("lawResults", lawResults);
        payload.put("caseResults", caseResults);
        return JSON.toJSONString(payload);
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

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String fallbackCaseAnalysis(DialogueContext context) {
        var caseFacts = context.getCaseFacts();
        if (caseFacts == null) {
            return "根据您描述的情况，我为您进行以下法律分析：";
        }

        return String.format("根据您提供的信息，这是一个%s类案件。\n\n【案情概述】\n%s",
                caseFacts.getCaseType(),
                caseFacts.getSummary() != null ? caseFacts.getSummary() : caseFacts.getSupplement());
    }

    private String fallbackSummary(DialogueContext context) {
        var caseFacts = context.getCaseFacts();
        return String.format("综合以上分析，对于您咨询的%s问题，建议先固定证据链并尽快推进法律程序。",
                caseFacts != null ? caseFacts.getCaseType() : "法律");
    }

    private String fallbackNextSteps() {
        return """
                1. 立即整理并备份证据（合同、转账、聊天记录、录音等）。
                2. 与对方进行一次留痕沟通，明确诉求与期限。
                3. 评估诉讼/仲裁路径，必要时尽快委托律师。
                4. 注意诉讼时效，避免权利过期。
                """;
    }

    private String defaultPrompt() {
        return """
你是法律结论生成助手。你将收到JSON输入，包含案情事实、法律法规结果、相似案例结果。
请基于这些信息生成结论，必须输出JSON且仅输出JSON：
{
  "caseAnalysis": "字符串，案情分析",
  "summary": "字符串，总结结论",
  "nextSteps": "字符串，分点建议"
}
要求：
1) 明确引用输入事实，不要杜撰关键事实。
2) 给出可执行建议，语气专业克制。
3) 结尾包含“以上分析仅供参考，不构成正式法律意见”。
""";
    }
}

