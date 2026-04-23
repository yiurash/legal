package com.feldsher.ai.test;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.feldsher.ai.service.LLMDialogueService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
public class LLMDialogueTest implements CommandLineRunner {

    @Autowired
    private LLMDialogueService llmDialogueService;

    private final Scanner scanner = new Scanner(System.in);

    private String currentSessionId;

    private List<PendingQuestion> pendingQuestions = new ArrayList<>();

    private boolean waitingForAnswers = false;

    private StringBuilder accumulatedResponse = new StringBuilder();

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class PendingQuestion {
        private String id;
        private String question;
        private List<String> options;
        private String userAnswer;
    }

    @Override
    public void run(String... args) {
        printWelcomeMessage();
        
        while (true) {
            try {
                if (waitingForAnswers && !pendingQuestions.isEmpty()) {
                    System.out.print("\n\033[1;34m请选择答案编号（用逗号分隔，如：1,2,3）: \033[0m");
                } else {
                    System.out.print("\n\033[1;34m用户: \033[0m");
                }
                
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

                if (waitingForAnswers && !pendingQuestions.isEmpty()) {
                    processUserAnswers(input);
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

    private void printWelcomeMessage() {
        System.out.println("\n" +
                "╔══════════════════════════════════════════════════════════════╗\n" +
                "║                   法律助手 AI 对话系统 (LLM驱动版)             ║\n" +
                "║                                                                  ║\n" +
                "║  当前模式: 真正流式输出 (LLM逐Token)                            ║\n" +
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
        System.out.println("  示例问题：");
        System.out.println("    • 我想离婚，请问财产怎么分？");
        System.out.println("    • 公司拖欠我三个月工资怎么办？");
        System.out.println("    • 我被骗了，应该怎么办？");
        System.out.println("    • 别人欠我钱不还怎么办？");
        System.out.println("\033[1;36m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\033[0m\n");
    }

    private void startNewDialogue() {
        System.out.println("\n\033[1;33m正在开始新的对话...\033[0m");
        
        if (currentSessionId != null) {
            llmDialogueService.removeSession(currentSessionId);
        }
        
        currentSessionId = llmDialogueService.createNewSession();
        pendingQuestions.clear();
        waitingForAnswers = false;
        accumulatedResponse.setLength(0);
        
        System.out.println("\033[1;32m新对话已创建，Session ID: " + currentSessionId + "\033[0m");
        System.out.println("\033[1;32m请输入您遇到的法律问题。\033[0m\n");
    }

    private void processUserInput(String userInput) {
        if (userInput.isEmpty()) {
            System.out.println("\n\033[1;33m请输入您的问题。\033[0m");
            return;
        }

        if (currentSessionId == null || !llmDialogueService.hasSession(currentSessionId)) {
            currentSessionId = llmDialogueService.createNewSession();
            System.out.println("\n\033[1;32m创建新对话，Session ID: " + currentSessionId + "\033[0m");
        }

        System.out.println("\n\033[1;35m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\033[0m");
        
        accumulatedResponse.setLength(0);
        
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<String> fullResponseRef = new AtomicReference<>();
        final AtomicReference<Throwable> errorRef = new AtomicReference<>();

        System.out.print("\033[1;90m🤔 深度思考中... \033[0m");
        long thinkingStartTime = System.currentTimeMillis();
        boolean[] firstTokenReceived = {false};

        llmDialogueService.chatWithStreaming(currentSessionId, userInput, new LLMDialogueService.SimpleStreamingCallback() {
            
            @Override
            public void onToken(String token) {
                accumulatedResponse.append(token);
                if (!firstTokenReceived[0]) {
                    long thinkingEndTime = System.currentTimeMillis();
                    double thinkingDuration = (thinkingEndTime - thinkingStartTime) / 1000.0;
                    System.out.printf(" \033[1;35m(%.1fs)\033[0m%n", thinkingDuration);
                    printAssistantSeparator();
                    firstTokenReceived[0] = true;
                }
                System.out.print(token);
            }

            @Override
            public void onComplete(String fullResponse) {
                System.out.println();
                fullResponseRef.set(fullResponse);
                latch.countDown();
            }

            @Override
            public void onError(Throwable error) {
                System.out.println("\n\033[1;31mLLM 调用出错: " + error.getMessage() + "\033[0m");
                errorRef.set(error);
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (errorRef.get() != null) {
            System.out.println("\033[1;35m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\033[0m");
            return;
        }

        String fullResponse = fullResponseRef.get();
        if (fullResponse != null) {
            parseAndHandleResponse(fullResponse);
        }

        System.out.println("\033[1;35m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\033[0m");
    }

    private void parseAndHandleResponse(String response) {
        String jsonStr = extractJson(response);
        
        if (jsonStr == null) {
            log.warn("无法从响应中提取 JSON，直接显示响应: {}", response);
            return;
        }

        try {
            JSONObject json = JSON.parseObject(jsonStr);
            String action = json.getString("action");

            if ("NON_LEGAL".equals(action)) {
                String message = json.getString("message");
                if (message != null) {
                    System.out.println("\n\033[1;33m[提示: " + message + "\033[0m");
                }
            } else if ("ASK_QUESTIONS".equals(action)) {
                handleAskQuestions(json);
            } else if ("GIVE_ADVICE".equals(action)) {
                handleGiveAdvice(json);
            }

        } catch (Exception e) {
            log.error("解析 JSON 响应出错", e);
        }
    }

    private void handleAskQuestions(JSONObject json) {
        String caseType = json.getString("caseType");
        String thinking = json.getString("thinking");
        
        if (thinking != null && !thinking.isEmpty()) {
            System.out.println("\n\033[1;90m💭 分析: " + thinking + "\033[0m");
        }

        JSONArray questionsArray = json.getJSONArray("questions");
        if (questionsArray == null || questionsArray.isEmpty()) {
            return;
        }

        pendingQuestions.clear();
        for (int i = 0; i < questionsArray.size(); i++) {
            JSONObject q = questionsArray.getJSONObject(i);
            String id = q.getString("id");
            String questionText = q.getString("question");
            JSONArray optionsArray = q.getJSONArray("options");
            List<String> options = optionsArray != null ? optionsArray.toJavaList(String.class) : new ArrayList<>();
            
            pendingQuestions.add(new PendingQuestion(id, questionText, options, null));
        }

        System.out.println("\n\033[1;36m【问题表单】\033[0m");
        System.out.println("\033[1;36m案件类型: " + caseType + "\033[0m");
        System.out.println("\033[1;36m为了给您提供更准确的法律建议，请回答以下问题：\033[0m");
        System.out.println();

        for (int i = 0; i < pendingQuestions.size(); i++) {
            PendingQuestion q = pendingQuestions.get(i);
            System.out.println("  \033[1;36m问题 " + (i + 1) + ": " + q.getQuestion() + "\033[0m");
            for (int j = 0; j < q.getOptions().size(); j++) {
                System.out.println("     [" + (j + 1) + "] " + q.getOptions().get(j));
            }
            System.out.println();
        }

        System.out.println("\033[1;33m请按照格式回答问题，例如：1,3,2,1,3\033[0m");
        System.out.println("\033[1;33m每个数字对应问题的选项编号，用逗号分隔\033[0m");
        
        waitingForAnswers = true;
    }

    private void handleGiveAdvice(JSONObject json) {
        String caseAnalysis = json.getString("caseAnalysis");
        String thinking = json.getString("thinking");
        String summary = json.getString("summary");
        String nextSteps = json.getString("nextSteps");
        
        JSONArray lawsArray = json.getJSONArray("laws");

        if (thinking != null && !thinking.isEmpty()) {
            System.out.println("\n\033[1;90m💭 分析: " + thinking + "\033[0m");
        }

        if (caseAnalysis != null && !caseAnalysis.isEmpty()) {
            System.out.println("\n\033[1;36m【案情分析】\033[0m");
            printStreamingText(caseAnalysis);
        }

        if (lawsArray != null && !lawsArray.isEmpty()) {
            System.out.println("\n\033[1;36m【相关法律法规】\033[0m");
            System.out.println("\033[1;36m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\033[0m");

            for (int i = 0; i < lawsArray.size(); i++) {
                JSONObject law = lawsArray.getJSONObject(i);
                String lawName = law.getString("lawName");
                String articleNumber = law.getString("articleNumber");
                String content = law.getString("content");
                
                System.out.println("\n  \033[1;35m[" + (i + 1) + "] " + lawName + " " + articleNumber + "\033[0m");
                if (content != null) {
                    System.out.println();
                    printIndentedText(content, 4);
                }
            }
        }

        if (summary != null && !summary.isEmpty()) {
            System.out.println("\n\033[1;36m【总结】\033[0m");
            System.out.println("\033[1;36m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\033[0m");
            printStreamingText(summary);
        }

        if (nextSteps != null && !nextSteps.isEmpty()) {
            System.out.println("\n\033[1;36m【下一步建议】\033[0m");
            System.out.println("\033[1;36m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\033[0m");
            printStreamingText(nextSteps);
        }

        System.out.println("\n\033[1;33m⚠️ 以上分析仅供参考，不构成正式的法律意见。建议您在采取任何法律行动前，咨询专业律师的意见。\033[0m");
        
        waitingForAnswers = false;
        pendingQuestions.clear();
        
        System.out.println("\n\033[1;32m对话已完成！您可以：\033[0m");
        System.out.println("\033[1;32m  • 输入 /new 开始新的对话\033[0m");
        System.out.println("\033[1;32m  • 输入其他问题继续咨询\033[0m");
        System.out.println("\033[1;32m  • 输入 /exit 退出系统\033[0m");
    }

    private void processUserAnswers(String input) {
        System.out.println("\n\033[1;35m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\033[0m");

        int questionCount = pendingQuestions.size();
        String[] options = input.split(",");
        
        if (options.length < questionCount) {
            System.out.println("\n\033[1;31m回答数量不匹配！共有 " + questionCount + " 个问题，您只回答了 " + options.length + " 个。\033[0m");
            System.out.println("\033[1;33m请重新输入，例如：1,3,2,1,3\033[0m");
            return;
        }

        StringBuilder answersBuilder = new StringBuilder();
        answersBuilder.append("用户回答了以下问题：\n");
        
        for (int i = 0; i < questionCount; i++) {
            PendingQuestion q = pendingQuestions.get(i);
            String optionStr = options[i].trim();
            
            try {
                int optionIndex = Integer.parseInt(optionStr) - 1;
                if (optionIndex >= 0 && optionIndex < q.getOptions().size()) {
                    String selectedOption = q.getOptions().get(optionIndex);
                    q.setUserAnswer(selectedOption);
                    answersBuilder.append("问题").append(i + 1).append(": ").append(q.getQuestion()).append("\n");
                    answersBuilder.append("  回答: ").append(selectedOption).append("\n");
                }
            } catch (NumberFormatException e) {
                System.out.println("\n\033[1;31m选项格式错误：" + optionStr + "，请输入数字编号。\033[0m");
                return;
            }
        }

        System.out.print("\n\033[1;33m是否有补充说明？（直接输入内容，或按回车跳过）\033[0m ");
        String supplement = scanner.nextLine().trim();
        
        if (!supplement.isEmpty()) {
            answersBuilder.append("\n补充说明: ").append(supplement);
        }

        String followUpMessage = answersBuilder.toString() + "\n\n请基于以上信息，给出法律建议。";
        
        processUserInput(followUpMessage);
    }

    private String extractJson(String response) {
        if (response == null) return null;
        
        int start = response.indexOf("{");
        if (start == -1) return null;
        
        int end = response.lastIndexOf("}");
        if (end == -1) return null;
        
        if (start < end) {
            return response.substring(start, end + 1);
        }
        
        return null;
    }

    private void printAssistantSeparator() {
        System.out.println();
        System.out.println("\033[1;32m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\033[0m");
        System.out.println("\033[1;32m🤖 法律助手\033[0m");
        System.out.println("\033[1;32m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\033[0m");
    }

    private void printStreamingText(String content) {
        if (content == null || content.isEmpty()) {
            return;
        }
        System.out.println(content);
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
}
