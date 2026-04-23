package com.feldsher.ai.test;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.feldsher.ai.agents.MasterAgent;
import com.feldsher.ai.service.DialogueService;
import com.feldsher.ai.service.RealTimeStreamingService;
import com.feldsher.common.context.DialogueContext;
import com.feldsher.common.dto.QuestionFormDTO;
import com.feldsher.common.vo.DialogueResponseVO;
import com.feldsher.common.vo.LawRetrievalVO;
import com.feldsher.common.vo.CaseRetrievalVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
// @Component 已被 LLMDialogueTest 取代，如需使用旧版请取消注释
public class ConsoleDialogueTest implements CommandLineRunner {

    @Autowired
    private DialogueService dialogueService;

    @Autowired(required = false)
    private RealTimeStreamingService realTimeStreamingService;

    @Value("${langchain4j.openai.api-key:}")
    private String apiKey;

    private final Scanner scanner = new Scanner(System.in);

    private DialogueContext currentContext;

    private boolean waitingForAnswers = false;

    private QuestionFormDTO pendingQuestionForm;

    private boolean useRealStreaming = false;

    @Override
    public void run(String... args) {
        checkStreamingMode();
        printWelcomeMessage();
        
        while (true) {
            try {
                System.out.print("\n\033[1;34m用户: \033[0m");
                String input = scanner.nextLine().trim();

                if ("/exit".equalsIgnoreCase(input)) {
                    printExitMessage();
                    break;
                }

                if ("/new".equalsIgnoreCase(input)) {
                    startNewDialogue();
                    continue;
                }

                if ("/help".equalsIgnoreCase(input)) {
                    printHelpMessage();
                    continue;
                }

                if (waitingForAnswers && pendingQuestionForm != null) {
                    processAnswers(input);
                } else {
                    processUserInput(input);
                }

            } catch (Exception e) {
                System.out.println("\n\033[1;31m错误: " + e.getMessage() + "\033[0m");
                log.error("处理对话出错", e);
            }
        }

        scanner.close();
    }

    private void checkStreamingMode() {
        if (apiKey != null && !apiKey.isEmpty() && !apiKey.startsWith("sk-your")) {
            useRealStreaming = true;
            log.info("检测到有效的 API Key，将使用真正的流式输出");
        } else {
            useRealStreaming = false;
            log.info("未检测到有效的 API Key，将使用模拟流式输出");
        }
    }

    private void printWelcomeMessage() {
        String mode = useRealStreaming ? "真正流式输出 (LLM逐Token)" : "模拟流式输出";
        System.out.println("\n" +
                "╔══════════════════════════════════════════════════════════════╗\n" +
                "║                   法律助手 AI 对话系统                          ║\n" +
                "║                                                                  ║\n" +
                "║  当前模式: " + mode + "                                           ║\n" +
                "║                                                                  ║\n" +
                "║  您可以咨询以下类型的法律问题：                                  ║\n" +
                "║  • 婚姻家事（离婚、子女抚养、财产分割等）                      ║\n" +
                "║  • 劳务纠纷（工资拖欠、违法解除、工伤等）                       ║\n" +
                "║  • 刑事诉讼（取保候审、醉驾、故意伤害等）                        ║\n" +
                "║  • 合同纠纷（民间借贷、买卖合同、租赁合同等）                     ║\n" +
                "║                                                                  ║\n" +
                "║  特殊命令：                                                      ║\n" +
                "║  • /new  - 开始新的对话                                         ║\n" +
                "║  • /exit - 退出系统                                             ║\n" +
                "║  • /help - 显示帮助信息                                         ║\n" +
                "║                                                                  ║\n" +
                "║  请输入您遇到的法律问题，我将为您提供专业的法律建议。             ║\n" +
                "╚══════════════════════════════════════════════════════════════╝\n");
    }

    private void printExitMessage() {
        System.out.println("\n\033[1;32m感谢使用法律助手 AI！如有其他问题，随时可以再来咨询。\033[0m");
        System.out.println("\033[1;32m祝您生活愉快，再见！\033[0m\n");
    }

    private void printHelpMessage() {
        System.out.println("\n\033[1;36m【帮助信息】\033[0m");
        System.out.println("\033[1;36m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\033[0m");
        System.out.println("  特殊命令：");
        System.out.println("    /new  - 开始新的对话（清除当前上下文）");
        System.out.println("    /exit - 退出系统");
        System.out.println("    /help - 显示此帮助信息");
        System.out.println();
        System.out.println("  使用说明：");
        System.out.println("    1. 直接输入您遇到的法律问题");
        System.out.println("    2. 系统会先进行意图分析，然后向您询问相关问题");
        System.out.println("    3. 请按照提示回答问题，每个问题选择一个选项编号");
        System.out.println("    4. 回答完所有问题后，系统会给出法律建议");
        System.out.println("    5. 建议会包含：案情分析、相关法规、相似案例、下一步建议");
        System.out.println();
        System.out.println("  示例问题：");
        System.out.println("    • 我想离婚，请问财产怎么分？");
        System.out.println("    • 公司拖欠我三个月工资怎么办？");
        System.out.println("    • 醉驾被查会怎么判？");
        System.out.println("    • 别人欠我钱不还怎么办？");
        System.out.println("\033[1;36m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\033[0m\n");
    }

    private void startNewDialogue() {
        System.out.println("\n\033[1;33m正在开始新的对话...\033[0m");
        currentContext = dialogueService.createNewSession();
        waitingForAnswers = false;
        pendingQuestionForm = null;
        System.out.println("\033[1;32m新对话已创建，Session ID: " + currentContext.getSessionId() + "\033[0m");
        System.out.println("\033[1;32m请输入您遇到的法律问题。\033[0m\n");
    }

    private void processUserInput(String userInput) {
        if (userInput.isEmpty()) {
            System.out.println("\n\033[1;33m请输入您的问题。\033[0m");
            return;
        }

        if (currentContext == null) {
            currentContext = dialogueService.createNewSession();
            System.out.println("\n\033[1;32m创建新对话，Session ID: " + currentContext.getSessionId() + "\033[0m");
        }

        System.out.println("\n\033[1;35m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\033[0m");

        MasterAgent.MasterAgentResult result = dialogueService.processUserInput(
                currentContext.getSessionId(), userInput);

        if (!result.getSuccess()) {
            System.out.println("\n\033[1;31m处理失败: " + result.getErrorMessage() + "\033[0m");
            return;
        }

        if (Boolean.TRUE.equals(result.getIsNonLegalQuestion())) {
            String thinkingContent = "检测到非法律问题，准备回复...";
            String replyContent = result.getThinkingContent();
            
            if (useRealStreaming && realTimeStreamingService != null) {
                streamThinkingAndReply(thinkingContent, replyContent);
            } else {
                simulateStreamingThinkingAndReply(thinkingContent, replyContent);
            }
            System.out.println("\033[1;35m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\033[0m");
            return;
        }

        if (result.getQuestionForm() != null) {
            String thinkingContent = result.getThinkingContent();
            if (thinkingContent == null || thinkingContent.isEmpty()) {
                thinkingContent = "正在分析您的问题...识别案件类型和相关法律领域...";
            }
            
            String assistantReply = generateAssistantReply(result.getQuestionForm());
            
            if (useRealStreaming && realTimeStreamingService != null) {
                streamThinkingAndReply(thinkingContent, assistantReply);
            } else {
                simulateStreamingThinkingAndReply(thinkingContent, assistantReply);
            }

            System.out.println("\n\033[1;36m【问题表单】\033[0m");
            System.out.println("\033[1;36m为了给您提供更准确的法律建议，请回答以下问题：\033[0m");
            System.out.println();

            pendingQuestionForm = result.getQuestionForm();
            displayQuestionForm(pendingQuestionForm);
            waitingForAnswers = true;

            System.out.println("\033[1;33m请按照格式回答问题，例如：1,3,2,1,3\033[0m");
            System.out.println("\033[1;33m每个数字对应问题的选项编号，用逗号分隔\033[0m");
        } else if (result.getConclusion() != null) {
            displayConclusion(result.getConclusion());
            waitingForAnswers = false;
            pendingQuestionForm = null;
        }

        System.out.println("\033[1;35m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\033[0m");
    }

    private void streamThinkingAndReply(String thinkingContent, String replyContent) {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Long> thinkingStartTime = new AtomicReference<>();
        final AtomicReference<Long> replyStartTime = new AtomicReference<>();

        realTimeStreamingService.streamThinkingAndReply(thinkingContent, replyContent, 
                new RealTimeStreamingService.SimpleStreamingCallback() {
            
            @Override
            public void onThinkingStart() {
                System.out.print("\033[1;90m🤔 深度思考中... \033[0m");
                thinkingStartTime.set(System.currentTimeMillis());
            }

            @Override
            public void onThinkingToken(String token) {
                System.out.print(token);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            @Override
            public void onContentStart() {
                long thinkingEndTime = System.currentTimeMillis();
                double thinkingDuration = (thinkingEndTime - thinkingStartTime.get()) / 1000.0;
                System.out.printf(" \033[1;35m(%.1fs)\033[0m%n", thinkingDuration);
                
                printAssistantSeparator();
                System.out.println();
                replyStartTime.set(System.currentTimeMillis());
            }

            @Override
            public void onContentToken(String token) {
                System.out.print(token);
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            @Override
            public void onComplete(RealTimeStreamingService.StreamingResult result) {
                long replyEndTime = System.currentTimeMillis();
                if (replyStartTime.get() != null) {
                    double replyDuration = (replyEndTime - replyStartTime.get()) / 1000.0;
                    System.out.println();
                }
                latch.countDown();
            }

            @Override
            public void onError(Throwable error) {
                System.out.println("\n\033[1;31m流式输出出错: " + error.getMessage() + "\033[0m");
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void simulateStreamingThinkingAndReply(String thinkingContent, String replyContent) {
        System.out.print("\033[1;90m🤔 深度思考中... \033[0m");
        long thinkingStartTime = System.currentTimeMillis();
        
        try {
            for (char c : thinkingContent.toCharArray()) {
                System.out.print(c);
                Thread.sleep(30);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        long thinkingEndTime = System.currentTimeMillis();
        double thinkingDuration = (thinkingEndTime - thinkingStartTime) / 1000.0;
        System.out.printf(" \033[1;35m(%.1fs)\033[0m%n", thinkingDuration);

        printAssistantSeparator();
        System.out.println();

        try {
            for (char c : replyContent.toCharArray()) {
                System.out.print(c);
                Thread.sleep(40);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println();
    }

    private String generateAssistantReply(QuestionFormDTO form) {
        String caseType = form.getCaseType();
        return "根据您的描述，这看起来是一个" + caseType + "类案件。为了给您提供更准确的法律建议，我需要了解一些具体信息，请回答下列问题：";
    }

    private void processAnswers(String input) {
        System.out.println("\n\033[1;35m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\033[0m");

        List<QuestionFormDTO.QuestionItem> questions = pendingQuestionForm.getQuestions();
        int questionCount = questions.size();

        String[] options = input.split(",");
        
        if (options.length < questionCount) {
            System.out.println("\n\033[1;31m回答数量不匹配！共有 " + questionCount + " 个问题，您只回答了 " + options.length + " 个。\033[0m");
            System.out.println("\033[1;33m请重新输入，例如：1,3,2,1,3\033[0m");
            return;
        }

        if (options.length > questionCount) {
            System.out.println("\n\033[1;31m回答数量过多！共有 " + questionCount + " 个问题，您回答了 " + options.length + " 个。\033[0m");
            System.out.println("\033[1;33m请重新输入，例如：1,3,2,1,3\033[0m");
            return;
        }

        for (int i = 0; i < questionCount; i++) {
            options[i] = options[i].trim();
        }

        Map<String, String> answersMap = new HashMap<>();
        StringBuilder answersDisplay = new StringBuilder();

        for (int i = 0; i < questions.size(); i++) {
            QuestionFormDTO.QuestionItem q = questions.get(i);
            int optionIndex;
            try {
                optionIndex = Integer.parseInt(options[i].trim()) - 1;
            } catch (NumberFormatException e) {
                System.out.println("\n\033[1;31m选项格式错误：" + options[i] + "，请输入数字编号。\033[0m");
                return;
            }

            if (optionIndex < 0 || optionIndex >= q.getOptions().size()) {
                System.out.println("\n\033[1;31m问题 " + (i + 1) + " 的选项编号超出范围，有效范围是 1-" + q.getOptions().size() + "\033[0m");
                return;
            }

            String selectedOption = q.getOptions().get(optionIndex);
            answersMap.put(q.getId(), selectedOption);
            answersDisplay.append("  ").append(i + 1).append(". ").append(q.getQuestion()).append("\n");
            answersDisplay.append("     选择: ").append(selectedOption).append("\n");
        }

        System.out.println("\n\033[1;36m【您的回答】\033[0m");
        System.out.println(answersDisplay);

        System.out.print("\n\033[1;33m是否有补充说明？（直接输入内容，或按回车跳过）\033[0m ");
        String supplement = scanner.nextLine().trim();

        if (!supplement.isEmpty()) {
            System.out.println("  补充说明: " + supplement);
        }

        String thinkingContent = "正在根据您提供的信息进行法律分析...正在检索相关的法律法规...正在查找相似的司法案例...";
        
        if (useRealStreaming && realTimeStreamingService != null) {
            streamThinkingOnly(thinkingContent);
        } else {
            simulateStreamingThinking(thinkingContent);
        }

        MasterAgent.MasterAgentResult result = dialogueService.processUserAnswers(
                currentContext.getSessionId(), answersMap, supplement);

        if (!result.getSuccess()) {
            System.out.println("\n\033[1;31m处理失败: " + result.getErrorMessage() + "\033[0m");
            return;
        }

        if (result.getConclusion() != null) {
            displayConclusion(result.getConclusion());
        }

        waitingForAnswers = false;
        pendingQuestionForm = null;

        System.out.println("\033[1;35m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\033[0m");
        System.out.println("\n\033[1;32m对话已完成！您可以：\033[0m");
        System.out.println("\033[1;32m  • 输入 /new 开始新的对话\033[0m");
        System.out.println("\033[1;32m  • 输入其他问题继续咨询\033[0m");
        System.out.println("\033[1;32m  • 输入 /exit 退出系统\033[0m");
    }

    private void streamThinkingOnly(String thinkingContent) {
        final CountDownLatch latch = new CountDownLatch(1);
        
        System.out.print("\033[1;90m🤔 深度思考中... \033[0m");
        long startTime = System.currentTimeMillis();
        
        try {
            for (char c : thinkingContent.toCharArray()) {
                System.out.print(c);
                Thread.sleep(30);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        long endTime = System.currentTimeMillis();
        double duration = (endTime - startTime) / 1000.0;
        System.out.printf(" \033[1;35m(%.1fs)\033[0m%n", duration);
    }

    private void simulateStreamingThinking(String thinkingContent) {
        System.out.print("\033[1;90m🤔 深度思考中... \033[0m");
        long startTime = System.currentTimeMillis();
        
        try {
            for (char c : thinkingContent.toCharArray()) {
                System.out.print(c);
                Thread.sleep(30);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        long endTime = System.currentTimeMillis();
        double duration = (endTime - startTime) / 1000.0;
        System.out.printf(" \033[1;35m(%.1fs)\033[0m%n", duration);
    }

    private void displayQuestionForm(QuestionFormDTO form) {
        List<QuestionFormDTO.QuestionItem> questions = form.getQuestions();

        System.out.println("  案件类型: " + form.getCaseType());
        System.out.println();

        for (int i = 0; i < questions.size(); i++) {
            QuestionFormDTO.QuestionItem q = questions.get(i);
            System.out.println("  \033[1;36m问题 " + (i + 1) + ": " + q.getQuestion() + "\033[0m");

            for (int j = 0; j < q.getOptions().size(); j++) {
                System.out.println("     [" + (j + 1) + "] " + q.getOptions().get(j));
            }
            System.out.println();
        }

        if (form.getHasSupplement()) {
            System.out.println("  \033[1;33m补充说明: " + form.getSupplementHint() + "\033[0m");
            System.out.println("  \033[1;33m（回答格式：选项编号,选项编号...,补充说明内容）\033[0m");
        }
    }

    private void displayConclusion(DialogueResponseVO.FinalConclusion conclusion) {
        printAssistantSeparator();

        if (conclusion.getCaseAnalysis() != null) {
            System.out.println("\n\033[1;36m【案情分析】\033[0m");
            printStreamingText(conclusion.getCaseAnalysis());
        }

        if (conclusion.getLawResults() != null && !conclusion.getLawResults().isEmpty()) {
            System.out.println("\n\033[1;36m【相关法律法规】\033[0m");
            System.out.println("\033[1;36m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\033[0m");

            for (int i = 0; i < conclusion.getLawResults().size(); i++) {
                LawRetrievalVO law = conclusion.getLawResults().get(i);
                System.out.println("\n  \033[1;35m[" + (i + 1) + "] " + law.getLawName() + " " + law.getArticleNumber() + "\033[0m");
                System.out.println("  " + law.getArticleTitle());
                System.out.println();
                System.out.println("  \033[1;33m法条内容:\033[0m");
                printIndentedText(law.getContent(), 4);
                System.out.println();
                System.out.println("  \033[1;33m关联性分析:\033[0m");
                printIndentedText(law.getRelevanceAnalysis(), 4);
                System.out.println();
                System.out.println("  \033[1;33m核心要点:\033[0m");
                printIndentedText(law.getKeyPoints(), 4);
            }
        }

        if (conclusion.getCaseResults() != null && !conclusion.getCaseResults().isEmpty()) {
            System.out.println("\n\033[1;36m【相似案例】\033[0m");
            System.out.println("\033[1;36m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\033[0m");

            for (int i = 0; i < conclusion.getCaseResults().size(); i++) {
                CaseRetrievalVO caseInfo = conclusion.getCaseResults().get(i);
                System.out.println("\n  \033[1;35m[" + (i + 1) + "] " + caseInfo.getCaseName() + "\033[0m");
                System.out.println("  法院: " + caseInfo.getCourt());
                System.out.println("  案号: " + caseInfo.getCaseNumber());
                System.out.println("  裁判日期: " + caseInfo.getJudgmentDate());
                System.out.println("  相似度: " + (int)(caseInfo.getRelevanceScore() * 100) + "%");
                System.out.println();
                System.out.println("  \033[1;33m基本案情:\033[0m");
                printIndentedText(caseInfo.getBasicFacts(), 4);
                System.out.println();
                System.out.println("  \033[1;33m法院认定:\033[0m");
                printIndentedText(caseInfo.getCourtFinding(), 4);
                System.out.println();
                System.out.println("  \033[1;33m裁判结果:\033[0m");
                printIndentedText(caseInfo.getJudgmentResult(), 4);
                System.out.println();
                System.out.println("  \033[1;33m关联性分析:\033[0m");
                printIndentedText(caseInfo.getRelevanceAnalysis(), 4);
                System.out.println();
                System.out.println("  \033[1;33m核心要点:\033[0m");
                if (caseInfo.getKeyTakeaways() != null) {
                    for (String point : caseInfo.getKeyTakeaways()) {
                        System.out.println("    • " + point);
                    }
                }
            }
        }

        if (conclusion.getSummary() != null) {
            System.out.println("\n\033[1;36m【总结】\033[0m");
            System.out.println("\033[1;36m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\033[0m");
            printStreamingText(conclusion.getSummary());
        }

        if (conclusion.getNextSteps() != null) {
            System.out.println("\n\033[1;36m【下一步建议】\033[0m");
            System.out.println("\033[1;36m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\033[0m");
            printStreamingText(conclusion.getNextSteps());
        }
    }

    private void printStreamingText(String content) {
        if (content == null || content.isEmpty()) {
            return;
        }
        try {
            for (char c : content.toCharArray()) {
                System.out.print(c);
                Thread.sleep(40);
            }
            System.out.println();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println(content);
        }
    }

    private void printIndentedText(String content, int indent) {
        if (content == null || content.isEmpty()) {
            return;
        }
        String indentStr = " ".repeat(indent);
        String[] lines = content.split("\n");
        for (String line : lines) {
            System.out.println(indentStr + line);
        }
    }

    private void printAssistantSeparator() {
        System.out.println();
        System.out.println("\033[1;32m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\033[0m");
        System.out.println("\033[1;32m🤖 法律助手\033[0m");
        System.out.println("\033[1;32m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\033[0m");
    }
}
