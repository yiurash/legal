package com.feldsher.ai.tools;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.feldsher.ai.service.PromptProvider;
import com.feldsher.ai.service.RealTimeStreamingService;
import com.feldsher.common.vo.LawRetrievalVO;
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
public class LawSearchTool {

    private static final String PROMPT_KEY = "tools/other_law_search";

    private final RealTimeStreamingService realTimeStreamingService;
    private final PromptProvider promptProvider;

    @Tool("根据案情事实和争议焦点，检索相关的法律法规和司法解释")
    public LawSearchResult searchLaws(
            @P("案由类型代码，如marriage_family、labor_dispute等") String caseTypeCode,
            @P("具体的案情事实描述") String caseFacts,
            @P("争议焦点列表，JSON格式") String disputeFocus) {
        
        log.info("开始检索法律法规，案由: {}, 案情: {}", caseTypeCode, caseFacts);

        LawSearchResult result = LawSearchResult.builder()
                .success(true)
                .totalResults(0)
                .results(new ArrayList<>())
                .build();

        try {
            List<LawRetrievalVO> laws = generateLawsByCaseType(caseTypeCode, caseFacts);
            result.setResults(laws);
            result.setTotalResults(laws.size());
            result.setSearchQuery("案由: " + caseTypeCode + ", 事实: " + caseFacts);

            log.info("法律法规检索完成，共找到 {} 条相关法规", laws.size());
        } catch (Exception e) {
            log.error("法律法规检索失败", e);
            result.setSuccess(false);
            result.setErrorMessage("法律法规检索失败: " + e.getMessage());
        }

        return result;
    }

    private List<LawRetrievalVO> generateLawsByCaseType(String caseTypeCode, String caseFacts) {
        List<LawRetrievalVO> laws = new ArrayList<>();

        switch (caseTypeCode) {
            case "marriage_family":
                laws = generateMarriageFamilyLaws(caseFacts);
                break;
            case "labor_dispute":
                laws = generateLaborDisputeLaws(caseFacts);
                break;
            case "criminal_procedure":
                laws = generateCriminalProcedureLaws(caseFacts);
                break;
            case "contract_dispute":
                laws = generateContractDisputeLaws(caseFacts);
                break;
            default:
                laws = generateGeneralLawsByLlm(caseFacts);
        }

        return laws;
    }

    private List<LawRetrievalVO> generateGeneralLawsByLlm(String caseFacts) {
        String systemPrompt = promptProvider.getPrompt(PROMPT_KEY, defaultPrompt());

        long start = System.currentTimeMillis();
        RealTimeStreamingService.StreamingResult llmResult =
                realTimeStreamingService.chatSync(systemPrompt, "案情事实: " + caseFacts);
        long duration = System.currentTimeMillis() - start;
        int outputLen = llmResult != null && llmResult.getResponseContent() != null ? llmResult.getResponseContent().length() : 0;
        log.info("LLM法规检索完成。prompt_key={}, duration_ms={}, output_len={}", PROMPT_KEY, duration, outputLen);
        if (llmResult == null || !llmResult.isSuccess()) {
            log.warn("LLM法规检索失败，回退到通用模板。prompt_key={}", PROMPT_KEY);
            return generateGeneralLaws(caseFacts);
        }

        try {
            String jsonText = extractJson(llmResult.getResponseContent());
            if (jsonText == null) {
                return generateGeneralLaws(caseFacts);
            }
            JSONObject root = JSON.parseObject(jsonText);
            JSONArray lawsArray = root.getJSONArray("laws");
            if (lawsArray == null || lawsArray.isEmpty()) {
                return generateGeneralLaws(caseFacts);
            }

            List<LawRetrievalVO> laws = new ArrayList<>();
            for (int i = 0; i < lawsArray.size(); i++) {
                JSONObject item = lawsArray.getJSONObject(i);
                laws.add(LawRetrievalVO.builder()
                        .lawId("LLM_LAW_" + (i + 1))
                        .lawName(item.getString("lawName"))
                        .articleNumber(item.getString("articleNumber"))
                        .articleTitle(item.getString("articleTitle"))
                        .content(item.getString("content"))
                        .relevanceAnalysis(item.getString("relevanceAnalysis"))
                        .keyPoints(item.getString("keyPoints"))
                        .build());
            }
            return laws.isEmpty() ? generateGeneralLaws(caseFacts) : laws;
        } catch (Exception e) {
            log.warn("LLM法规检索解析失败，回退到通用模板: {}", e.getMessage());
            return generateGeneralLaws(caseFacts);
        }
    }

    private String defaultPrompt() {
        return """
返回JSON: {"laws":[{"lawName":"法律名称","articleNumber":"第X条"}]}。
""";
    }

    private List<LawRetrievalVO> generateMarriageFamilyLaws(String caseFacts) {
        List<LawRetrievalVO> laws = new ArrayList<>();

        String lowerFacts = caseFacts.toLowerCase();

        if (lowerFacts.contains("离婚") || lowerFacts.contains("婚姻")) {
            laws.add(LawRetrievalVO.builder()
                    .lawId("LAW_001")
                    .lawName("中华人民共和国民法典")
                    .articleNumber("第一千零七十九条")
                    .articleTitle("诉讼离婚")
                    .content("夫妻一方要求离婚的，可以由有关组织进行调解或者直接向人民法院提起离婚诉讼。人民法院审理离婚案件，应当进行调解；如果感情确已破裂，调解无效的，应当准予离婚。有下列情形之一，调解无效的，应当准予离婚：（一）重婚或者与他人同居；（二）实施家庭暴力或者虐待、遗弃家庭成员；（三）有赌博、吸毒等恶习屡教不改；（四）因感情不和分居满二年；（五）其他导致夫妻感情破裂的情形。")
                    .relevanceAnalysis("本条是关于诉讼离婚的核心规定。根据您描述的情况，如果存在因感情不和分居满二年、家庭暴力、赌博吸毒等情形，法院调解无效的，应当准予离婚。")
                    .keyPoints("1. 诉讼离婚需要向法院提起；2. 法院审理离婚案件应当先调解；3. 法定离婚情形包括重婚、家暴、赌博吸毒、分居满二年等。")
                    .build());
        }

        if (lowerFacts.contains("子女") || lowerFacts.contains("抚养") || lowerFacts.contains("抚养权")) {
            laws.add(LawRetrievalVO.builder()
                    .lawId("LAW_002")
                    .lawName("中华人民共和国民法典")
                    .articleNumber("第一千零八十四条")
                    .articleTitle("离婚后的父母子女关系")
                    .content("父母与子女间的关系，不因父母离婚而消除。离婚后，子女无论由父或者母直接抚养，仍是父母双方的子女。离婚后，父母对于子女仍有抚养、教育、保护的权利和义务。离婚后，不满两周岁的子女，以由母亲直接抚养为原则。已满两周岁的子女，父母双方对抚养问题协议不成的，由人民法院根据双方的具体情况，按照最有利于未成年子女的原则判决。子女已满八周岁的，应当尊重其真实意愿。")
                    .relevanceAnalysis("本条规定了离婚后子女抚养的基本原则。不满两周岁的子女原则上由母亲抚养；已满两周岁的按照最有利于子女的原则判决；已满八周岁的要尊重其真实意愿。")
                    .keyPoints("1. 不满两周岁：母亲直接抚养为原则；2. 已满两周岁：最有利于未成年子女；3. 已满八周岁：尊重真实意愿。")
                    .build());
        }

        if (lowerFacts.contains("财产") || lowerFacts.contains("分割") || lowerFacts.contains("房产")) {
            laws.add(LawRetrievalVO.builder()
                    .lawId("LAW_003")
                    .lawName("中华人民共和国民法典")
                    .articleNumber("第一千零八十七条")
                    .articleTitle("离婚时夫妻共同财产的处理")
                    .content("离婚时，夫妻的共同财产由双方协议处理；协议不成的，由人民法院根据财产的具体情况，按照照顾子女、女方和无过错方权益的原则判决。对夫或者妻在家庭土地承包经营中享有的权益等，应当依法予以保护。")
                    .relevanceAnalysis("本条规定了离婚时夫妻共同财产的分割原则。首先由双方协议处理，协议不成的由法院判决。法院判决时会照顾子女、女方和无过错方的权益。")
                    .keyPoints("1. 先协议，协议不成由法院判决；2. 照顾子女、女方和无过错方；3. 土地承包权益也应保护。")
                    .build());
        }

        return laws;
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

    private List<LawRetrievalVO> generateLaborDisputeLaws(String caseFacts) {
        List<LawRetrievalVO> laws = new ArrayList<>();

        String lowerFacts = caseFacts.toLowerCase();

        if (lowerFacts.contains("工资") || lowerFacts.contains("拖欠") || lowerFacts.contains("报酬")) {
            laws.add(LawRetrievalVO.builder()
                    .lawId("LAW_001")
                    .lawName("中华人民共和国劳动合同法")
                    .articleNumber("第三十条")
                    .articleTitle("劳动报酬")
                    .content("用人单位应当按照劳动合同约定和国家规定，向劳动者及时足额支付劳动报酬。用人单位拖欠或者未足额支付劳动报酬的，劳动者可以依法向当地人民法院申请支付令，人民法院应当依法发出支付令。")
                    .relevanceAnalysis("本条规定了用人单位应当及时足额支付劳动报酬。如果用人单位拖欠工资，劳动者可以向法院申请支付令，这是一种快速维权的方式。")
                    .keyPoints("1. 用人单位应当及时足额支付工资；2. 拖欠工资的，劳动者可申请支付令；3. 支付令是快速维权途径。")
                    .build());
        }

        if (lowerFacts.contains("合同") || lowerFacts.contains("签订") || lowerFacts.contains("书面")) {
            laws.add(LawRetrievalVO.builder()
                    .lawId("LAW_002")
                    .lawName("中华人民共和国劳动合同法")
                    .articleNumber("第八十二条")
                    .articleTitle("不订立书面劳动合同的法律责任")
                    .content("用人单位自用工之日起超过一个月不满一年未与劳动者订立书面劳动合同的，应当向劳动者每月支付二倍的工资。用人单位违反本法规定不与劳动者订立无固定期限劳动合同的，自应当订立无固定期限劳动合同之日起向劳动者每月支付二倍的工资。")
                    .relevanceAnalysis("本条规定了未签订书面劳动合同的法律责任。用人单位超过一个月不满一年未签订劳动合同的，需要向劳动者支付二倍工资，最多支付11个月。")
                    .keyPoints("1. 超过1个月不满1年未签合同：二倍工资；2. 最多支付11个月；3. 仲裁时效从入职第2个月起算。")
                    .build());
        }

        if (lowerFacts.contains("解除") || lowerFacts.contains("辞退") || lowerFacts.contains("解雇")) {
            laws.add(LawRetrievalVO.builder()
                    .lawId("LAW_003")
                    .lawName("中华人民共和国劳动合同法")
                    .articleNumber("第四十七条")
                    .articleTitle("经济补偿的计算")
                    .content("经济补偿按劳动者在本单位工作的年限，每满一年支付一个月工资的标准向劳动者支付。六个月以上不满一年的，按一年计算；不满六个月的，向劳动者支付半个月工资的经济补偿。劳动者月工资高于用人单位所在直辖市、设区的市级人民政府公布的本地区上年度职工月平均工资三倍的，向其支付经济补偿的标准按职工月平均工资三倍的数额支付，向其支付经济补偿的年限最高不超过十二年。本条所称月工资是指劳动者在劳动合同解除或者终止前十二个月的平均工资。")
                    .relevanceAnalysis("本条规定了经济补偿的计算标准。如果用人单位合法解除劳动合同，需要支付经济补偿；如果是违法解除，则需要支付赔偿金（经济补偿的2倍）。")
                    .keyPoints("1. 每满1年支付1个月工资；2. 6个月以上不满1年按1年算；3. 月工资指解除前12个月平均工资。")
                    .build());
        }

        return laws;
    }

    private List<LawRetrievalVO> generateCriminalProcedureLaws(String caseFacts) {
        List<LawRetrievalVO> laws = new ArrayList<>();

        String lowerFacts = caseFacts.toLowerCase();

        if (lowerFacts.contains("取保") || lowerFacts.contains("拘留") || lowerFacts.contains("逮捕")) {
            laws.add(LawRetrievalVO.builder()
                    .lawId("LAW_001")
                    .lawName("中华人民共和国刑事诉讼法")
                    .articleNumber("第六十七条")
                    .articleTitle("取保候审的条件与执行")
                    .content("人民法院、人民检察院和公安机关对有下列情形之一的犯罪嫌疑人、被告人，可以取保候审：（一）可能判处管制、拘役或者独立适用附加刑的；（二）可能判处有期徒刑以上刑罚，采取取保候审不致发生社会危险性的；（三）患有严重疾病、生活不能自理，怀孕或者正在哺乳自己婴儿的妇女，采取取保候审不致发生社会危险性的；（四）羁押期限届满，案件尚未办结，需要采取取保候审的。取保候审由公安机关执行。")
                    .relevanceAnalysis("本条规定了取保候审的适用条件。如果您或您的亲友符合这些条件，可以向办案机关申请取保候审。")
                    .keyPoints("1. 可能判处管制、拘役或独立适用附加刑；2. 可能判处有期徒刑以上刑罚，但不致发生社会危险性；3. 患有严重疾病、怀孕或哺乳的妇女；4. 羁押期限届满。")
                    .build());
        }

        if (lowerFacts.contains("认罪") || lowerFacts.contains("认罚")) {
            laws.add(LawRetrievalVO.builder()
                    .lawId("LAW_002")
                    .lawName("中华人民共和国刑事诉讼法")
                    .articleNumber("第十五条")
                    .articleTitle("认罪认罚从宽制度")
                    .content("犯罪嫌疑人、被告人自愿如实供述自己的罪行，承认指控的犯罪事实，愿意接受处罚的，可以依法从宽处理。")
                    .relevanceAnalysis("本条确立了认罪认罚从宽制度。如果犯罪嫌疑人、被告人自愿认罪认罚，可以获得从宽处理，包括从轻、减轻处罚，以及适用速裁程序等。")
                    .keyPoints("1. 自愿如实供述罪行；2. 承认指控的犯罪事实；3. 愿意接受处罚；4. 可以依法从宽处理。")
                    .build());
        }

        if (lowerFacts.contains("律师") || lowerFacts.contains("会见") || lowerFacts.contains("辩护")) {
            laws.add(LawRetrievalVO.builder()
                    .lawId("LAW_003")
                    .lawName("中华人民共和国刑事诉讼法")
                    .articleNumber("第三十四条")
                    .articleTitle("委托辩护的时间")
                    .content("犯罪嫌疑人自被侦查机关第一次讯问或者采取强制措施之日起，有权委托辩护人；在侦查期间，只能委托律师作为辩护人。被告人有权随时委托辩护人。侦查机关在第一次讯问犯罪嫌疑人或者对犯罪嫌疑人采取强制措施的时候，应当告知犯罪嫌疑人有权委托辩护人。人民检察院自收到移送审查起诉的案件材料之日起三日以内，应当告知犯罪嫌疑人有权委托辩护人。人民法院自受理案件之日起三日以内，应当告知被告人有权委托辩护人。犯罪嫌疑人、被告人在押期间要求委托辩护人的，人民法院、人民检察院和公安机关应当及时转达其要求。")
                    .relevanceAnalysis("本条规定了委托辩护的时间。犯罪嫌疑人自被第一次讯问或采取强制措施之日起，就有权委托律师作为辩护人。在侦查期间，只能委托律师作为辩护人。")
                    .keyPoints("1. 第一次讯问或采取强制措施之日起可委托律师；2. 侦查期间只能委托律师；3. 办案机关应当告知有权委托辩护人。")
                    .build());
        }

        return laws;
    }

    private List<LawRetrievalVO> generateContractDisputeLaws(String caseFacts) {
        List<LawRetrievalVO> laws = new ArrayList<>();

        String lowerFacts = caseFacts.toLowerCase();

        if (lowerFacts.contains("违约") || lowerFacts.contains("不履行") || lowerFacts.contains("迟延")) {
            laws.add(LawRetrievalVO.builder()
                    .lawId("LAW_001")
                    .lawName("中华人民共和国民法典")
                    .articleNumber("第五百七十七条")
                    .articleTitle("违约责任的基本形态")
                    .content("当事人一方不履行合同义务或者履行合同义务不符合约定的，应当承担继续履行、采取补救措施或者赔偿损失等违约责任。")
                    .relevanceAnalysis("本条规定了违约责任的基本形态。如果对方不履行合同义务或者履行不符合约定，您可以要求对方继续履行、采取补救措施或者赔偿损失。")
                    .keyPoints("1. 不履行合同义务：承担违约责任；2. 履行不符合约定：承担违约责任；3. 责任形式：继续履行、补救措施、赔偿损失。")
                    .build());
        }

        if (lowerFacts.contains("解除") || lowerFacts.contains("终止")) {
            laws.add(LawRetrievalVO.builder()
                    .lawId("LAW_002")
                    .lawName("中华人民共和国民法典")
                    .articleNumber("第五百六十三条")
                    .articleTitle("合同法定解除")
                    .content("有下列情形之一的，当事人可以解除合同：（一）因不可抗力致使不能实现合同目的；（二）在履行期限届满前，当事人一方明确表示或者以自己的行为表明不履行主要债务；（三）当事人一方迟延履行主要债务，经催告后在合理期限内仍未履行；（四）当事人一方迟延履行债务或者有其他违约行为致使不能实现合同目的；（五）法律规定的其他情形。以持续履行的债务为内容的不定期合同，当事人可以随时解除合同，但是应当在合理期限之前通知对方。")
                    .relevanceAnalysis("本条规定了合同法定解除的情形。如果对方迟延履行主要债务，经催告后在合理期限内仍未履行，或者对方的违约行为致使不能实现合同目的，您可以解除合同。")
                    .keyPoints("1. 不可抗力致使不能实现合同目的；2. 明确表示不履行主要债务；3. 迟延履行经催告仍未履行；4. 违约致使不能实现合同目的。")
                    .build());
        }

        if (lowerFacts.contains("借款") || lowerFacts.contains("欠钱") || lowerFacts.contains("利息")) {
            laws.add(LawRetrievalVO.builder()
                    .lawId("LAW_003")
                    .lawName("中华人民共和国民法典")
                    .articleNumber("第六百七十六条")
                    .articleTitle("借款人逾期返还借款的责任")
                    .content("借款人未按照约定的期限返还借款的，应当按照约定或者国家有关规定支付逾期利息。")
                    .relevanceAnalysis("本条规定了借款人逾期还款的责任。如果借款人未按照约定期限返还借款，应当支付逾期利息。另外，根据相关规定，民间借贷的利率上限是合同成立时一年期LPR的四倍。")
                    .keyPoints("1. 逾期还款应当支付逾期利息；2. 民间借贷利率上限：LPR的四倍；3. 没有约定利息的视为无息，但逾期可主张逾期利息。")
                    .build());
        }

        return laws;
    }

    private List<LawRetrievalVO> generateGeneralLaws(String caseFacts) {
        List<LawRetrievalVO> laws = new ArrayList<>();

        laws.add(LawRetrievalVO.builder()
                .lawId("LAW_001")
                .lawName("中华人民共和国民法典")
                .articleNumber("第一百一十九条")
                .articleTitle("合同的约束力")
                .content("依法成立的合同，对当事人具有法律约束力。")
                .relevanceAnalysis("本条确立了合同的约束力原则。依法成立的合同，对各方当事人都具有法律约束力，各方都应当按照约定履行自己的义务。")
                .keyPoints("1. 依法成立的合同有法律约束力；2. 当事人应当按照约定履行义务。")
                .build());

        laws.add(LawRetrievalVO.builder()
                .lawId("LAW_002")
                .lawName("中华人民共和国民事诉讼法")
                .articleNumber("第六十七条")
                .articleTitle("举证责任")
                .content("当事人对自己提出的主张，有责任提供证据。当事人及其诉讼代理人因客观原因不能自行收集的证据，或者人民法院认为审理案件需要的证据，人民法院应当调查收集。人民法院应当按照法定程序，全面地、客观地审查核实证据。")
                .relevanceAnalysis("本条规定了民事诉讼中的举证责任原则。谁主张，谁举证。如果您要向法院起诉，需要提供证据证明您的主张。")
                .keyPoints("1. 谁主张，谁举证；2. 客观原因不能收集的，法院可调查收集；3. 法院应当全面客观审查核实证据。")
                .build());

        laws.add(LawRetrievalVO.builder()
                .lawId("LAW_003")
                .lawName("中华人民共和国民法典")
                .articleNumber("第一百八十八条")
                .articleTitle("普通诉讼时效、最长权利保护期间")
                .content("向人民法院请求保护民事权利的诉讼时效期间为三年。法律另有规定的，依照其规定。诉讼时效期间自权利人知道或者应当知道权利受到损害以及义务人之日起计算。法律另有规定的，依照其规定。但是，自权利受到损害之日起超过二十年的，人民法院不予保护，有特殊情况的，人民法院可以根据权利人的申请决定延长。")
                .relevanceAnalysis("本条规定了诉讼时效期间。一般民事纠纷的诉讼时效是三年，从您知道或应当知道权利受到损害以及义务人之日起计算。超过诉讼时效的，对方可以提出时效抗辩。")
                .keyPoints("1. 普通诉讼时效：三年；2. 起算点：知道或应当知道权利受损及义务人；3. 最长保护期：二十年；4. 超过时效可能丧失胜诉权。")
                .build());

        return laws;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LawSearchResult implements Serializable {
        private static final long serialVersionUID = 1L;

        private Boolean success;
        private String searchQuery;
        private Integer totalResults;
        private List<LawRetrievalVO> results;
        private String errorMessage;
    }
}
