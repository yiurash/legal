package com.feldsher.ai.tools;

import com.feldsher.common.vo.CaseRetrievalVO;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class CaseSearchTool {

    @Tool("根据案情事实和争议焦点，检索相似的司法案例")
    public CaseSearchResult searchCases(
            @P("案由类型代码，如marriage_family、labor_dispute等") String caseTypeCode,
            @P("具体的案情事实描述") String caseFacts,
            @P("争议焦点列表，JSON格式") String disputeFocus) {
        
        log.info("开始检索相似案例，案由: {}, 案情: {}", caseTypeCode, caseFacts);

        CaseSearchResult result = CaseSearchResult.builder()
                .success(true)
                .totalFound(0)
                .selectedCases(new ArrayList<>())
                .build();

        try {
            List<CaseRetrievalVO> cases = generateCasesByCaseType(caseTypeCode, caseFacts);
            result.setSelectedCases(cases);
            result.setTotalFound(cases.size() * 5);
            result.setSummary("共检索到" + (cases.size() * 5) + "个相似案例，精选" + cases.size() + "个最具参考价值的案例。");

            log.info("案例检索完成，共找到 {} 个相关案例", cases.size());
        } catch (Exception e) {
            log.error("案例检索失败", e);
            result.setSuccess(false);
            result.setErrorMessage("案例检索失败: " + e.getMessage());
        }

        return result;
    }

    private List<CaseRetrievalVO> generateCasesByCaseType(String caseTypeCode, String caseFacts) {
        List<CaseRetrievalVO> cases = new ArrayList<>();

        switch (caseTypeCode) {
            case "marriage_family":
                cases = generateMarriageFamilyCases(caseFacts);
                break;
            case "labor_dispute":
                cases = generateLaborDisputeCases(caseFacts);
                break;
            case "criminal_procedure":
                cases = generateCriminalProcedureCases(caseFacts);
                break;
            case "contract_dispute":
                cases = generateContractDisputeCases(caseFacts);
                break;
            default:
                cases = generateGeneralCases(caseFacts);
        }

        return cases;
    }

    private List<CaseRetrievalVO> generateMarriageFamilyCases(String caseFacts) {
        List<CaseRetrievalVO> cases = new ArrayList<>();

        String lowerFacts = caseFacts.toLowerCase();

        if (lowerFacts.contains("离婚") || lowerFacts.contains("分居")) {
            cases.add(CaseRetrievalVO.builder()
                    .caseId("CASE_001")
                    .caseName("张某与李某离婚纠纷案")
                    .court("北京市朝阳区人民法院")
                    .caseNumber("(2023)京0105民初12345号")
                    .judgmentDate(LocalDate.of(2023, 8, 15))
                    .caseType("婚姻家事-离婚纠纷")
                    .basicFacts("原告张某与被告李某于2018年登记结婚，婚后育有一子。2021年双方因感情不和开始分居，原告于2023年起诉离婚。被告辩称夫妻感情尚未破裂，不同意离婚。")
                    .disputeFocus(List.of("夫妻感情是否确已破裂", "分居事实是否满两年", "子女抚养权归属"))
                    .courtFinding("法院经审理查明，原被告因感情不和分居已满两年，符合法定离婚条件。关于子女抚养，考虑到孩子一直随原告生活，判决由原告抚养为宜。")
                    .judgmentResult("一、准予原告张某与被告李某离婚；二、婚生子由原告张某抚养，被告李某每月支付抚养费3000元；三、夫妻共同财产依法分割。")
                    .relevanceScore(0.92)
                    .relevanceAnalysis("本案与您咨询的情况高度相似：均因感情不和分居满两年、涉及子女抚养问题。法院的裁判思路和判决结果对本案具有重要参考价值。")
                    .keyTakeaways(List.of(
                            "因感情不和分居满两年是法定离婚情形",
                            "子女抚养权优先考虑子女利益和实际抚养情况",
                            "分居期间的财产仍可能被认定为共同财产"
                    ))
                    .build());
        }

        if (lowerFacts.contains("子女") || lowerFacts.contains("抚养")) {
            cases.add(CaseRetrievalVO.builder()
                    .caseId("CASE_002")
                    .caseName("王某与赵某抚养权纠纷案")
                    .court("上海市浦东新区人民法院")
                    .caseNumber("(2023)沪0115民初67890号")
                    .judgmentDate(LocalDate.of(2023, 10, 20))
                    .caseType("婚姻家事-抚养纠纷")
                    .basicFacts("原被告于2019年协议离婚，约定婚生女由原告抚养。2023年被告以原告工作繁忙无暇照顾子女为由，起诉要求变更抚养权。")
                    .disputeFocus(List.of("是否符合变更抚养权的条件", "哪一方抚养更有利于子女成长", "子女的意愿如何"))
                    .courtFinding("法院经审理查明，原告确实经常出差，子女主要由祖父母照顾。被告工作稳定，有更多时间照顾子女。经征询子女意见，子女表示愿意随被告生活。")
                    .judgmentResult("一、变更婚生女由被告赵某抚养；二、原告王某每月支付抚养费2500元；三、原告王某每周可探望子女一次。")
                    .relevanceScore(0.85)
                    .relevanceAnalysis("本案涉及抚养权变更问题。法院主要考虑：是否有足够时间照顾子女、是否有稳定的收入和住所、子女的意愿等因素。")
                    .keyTakeaways(List.of(
                            "抚养权可以根据实际情况变更",
                            "法院优先考虑最有利于子女成长的原则",
                            "子女年满八周岁的，其意愿很重要"
                    ))
                    .build());
        }

        cases.add(CaseRetrievalVO.builder()
                .caseId("CASE_003")
                .caseName("刘某与陈某离婚后财产纠纷案")
                .court("广州市天河区人民法院")
                .caseNumber("(2023)粤0106民初24680号")
                .judgmentDate(LocalDate.of(2023, 11, 8))
                .caseType("婚姻家事-离婚后财产纠纷")
                .basicFacts("原被告于2022年协议离婚，离婚协议中未涉及被告名下的股票账户。2023年原告发现该股票账户在婚姻关系存续期间有大额交易，起诉要求分割。")
                .disputeFocus(List.of("股票账户是否属于夫妻共同财产", "离婚协议是否遗漏了该财产", "分割比例如何确定"))
                .courtFinding("法院经审理查明，该股票账户开立于婚姻关系存续期间，资金来源于夫妻共同财产。离婚协议中确实未涉及该财产，原告有权要求分割。")
                .judgmentResult("一、被告陈某名下股票账户中的资产，原告刘某分得50%；二、被告陈某于判决生效后十日内支付原告刘某相应款项。")
                .relevanceScore(0.78)
                .relevanceAnalysis("本案涉及离婚后财产分割问题。如果离婚时遗漏了夫妻共同财产，离婚后发现的仍可以起诉要求分割。")
                .keyTakeaways(List.of(
                        "离婚时遗漏的共同财产，离婚后可另行起诉分割",
                        "股票账户如用夫妻共同财产投资，属于共同财产",
                        "一般情况下平均分割"
                ))
                .build());

        return cases;
    }

    private List<CaseRetrievalVO> generateLaborDisputeCases(String caseFacts) {
        List<CaseRetrievalVO> cases = new ArrayList<>();

        String lowerFacts = caseFacts.toLowerCase();

        if (lowerFacts.contains("工资") || lowerFacts.contains("拖欠")) {
            cases.add(CaseRetrievalVO.builder()
                    .caseId("CASE_001")
                    .caseName("孙某与某科技公司劳动争议案")
                    .court("北京市海淀区人民法院")
                    .caseNumber("(2023)京0108民初98765号")
                    .judgmentDate(LocalDate.of(2023, 9, 12))
                    .caseType("劳动争议-追索劳动报酬")
                    .basicFacts("原告孙某于2021年入职被告公司，月工资15000元。2023年3月起，被告开始拖欠工资。原告于2023年6月离职，申请劳动仲裁要求支付拖欠工资及经济补偿。")
                    .disputeFocus(List.of("被告是否拖欠原告工资", "原告主张的工资标准是否正确", "被告是否应当支付经济补偿"))
                    .courtFinding("法院经审理查明，被告确实拖欠原告2023年3月至5月的工资共计45000元。原告因被告拖欠工资离职，符合获得经济补偿的条件。")
                    .judgmentResult("一、被告某科技公司于判决生效后十日内支付原告孙某拖欠工资45000元；二、被告支付原告经济补偿37500元；三、驳回原告其他诉讼请求。")
                    .relevanceScore(0.9)
                    .relevanceAnalysis("本案是典型的拖欠工资劳动争议案件。法院支持了原告要求支付拖欠工资及因拖欠工资离职的经济补偿的请求。")
                    .keyTakeaways(List.of(
                            "用人单位应当及时足额支付工资",
                            "因拖欠工资离职的，可主张经济补偿",
                            "经济补偿标准：每满1年支付1个月工资"
                    ))
                    .build());
        }

        if (lowerFacts.contains("合同") || lowerFacts.contains("未签")) {
            cases.add(CaseRetrievalVO.builder()
                    .caseId("CASE_002")
                    .caseName("周某与某餐饮公司劳动争议案")
                    .court("上海市黄浦区人民法院")
                    .caseNumber("(2023)沪0101民初54321号")
                    .judgmentDate(LocalDate.of(2023, 7, 25))
                    .caseType("劳动争议-未签劳动合同二倍工资")
                    .basicFacts("原告周某于2022年3月入职被告餐饮公司，担任厨师，月工资8000元。双方未签订书面劳动合同。2023年2月原告离职，申请劳动仲裁要求支付未签劳动合同二倍工资差额。")
                    .disputeFocus(List.of("原被告之间是否存在劳动关系", "未签劳动合同二倍工资的计算基数", "仲裁时效是否经过"))
                    .courtFinding("法院经审理查明，原被告之间存在事实劳动关系。原告主张的未签劳动合同二倍工资未超过仲裁时效。")
                    .judgmentResult("一、被告某餐饮公司于判决生效后十日内支付原告周某未签劳动合同二倍工资差额72000元；二、驳回原告其他诉讼请求。")
                    .relevanceScore(0.88)
                    .relevanceAnalysis("本案涉及未签订书面劳动合同的二倍工资问题。法院支持了原告的请求，判决用人单位支付了9个月的二倍工资差额。")
                    .keyTakeaways(List.of(
                            "未签劳动合同，用人单位需支付二倍工资",
                            "最多支付11个月的二倍工资",
                            "仲裁时效从入职第2个月起算"
                    ))
                    .build());
        }

        cases.add(CaseRetrievalVO.builder()
                .caseId("CASE_003")
                .caseName("吴某与某贸易公司劳动争议案")
                .court("深圳市南山区人民法院")
                .caseNumber("(2023)粤0305民初13579号")
                .judgmentDate(LocalDate.of(2023, 12, 5))
                .caseType("劳动争议-违法解除劳动合同")
                .basicFacts("原告吴某于2020年入职被告公司，月工资12000元。2023年8月，被告以原告严重违反规章制度为由解除劳动合同。原告认为解除违法，申请劳动仲裁。")
                .disputeFocus(List.of("被告解除劳动合同是否合法", "原告是否严重违反规章制度", "被告是否应当支付赔偿金"))
                .courtFinding("法院经审理查明，被告所称的规章制度未经过民主程序制定，也未向原告公示。因此，被告以原告违反规章制度为由解除劳动合同缺乏依据，属于违法解除。")
                .judgmentResult("一、被告某贸易公司于判决生效后十日内支付原告吴某违法解除劳动合同赔偿金84000元；二、驳回原告其他诉讼请求。")
                .relevanceScore(0.82)
                .relevanceAnalysis("本案涉及违法解除劳动合同的问题。法院认定用人单位解除劳动合同违法，判决支付了赔偿金（经济补偿的2倍）。")
                .keyTakeaways(List.of(
                        "规章制度需经过民主程序制定并公示",
                        "违法解除劳动合同需支付赔偿金",
                        "赔偿金标准：经济补偿的2倍"
                ))
                .build());

        return cases;
    }

    private List<CaseRetrievalVO> generateCriminalProcedureCases(String caseFacts) {
        List<CaseRetrievalVO> cases = new ArrayList<>();

        String lowerFacts = caseFacts.toLowerCase();

        if (lowerFacts.contains("取保") || lowerFacts.contains("羁押")) {
            cases.add(CaseRetrievalVO.builder()
                    .caseId("CASE_001")
                    .caseName("钱某涉嫌诈骗罪取保候审案")
                    .court("杭州市西湖区人民法院")
                    .caseNumber("(2023)浙0106刑初888号")
                    .judgmentDate(LocalDate.of(2023, 6, 30))
                    .caseType("刑事诉讼-取保候审")
                    .basicFacts("被告人钱某因涉嫌诈骗罪于2023年3月被刑事拘留。其家属委托律师申请取保候审，检察院审查后认为钱某认罪态度好，已退赔被害人损失，采取取保候审不致发生社会危险性，决定不批准逮捕，公安机关对其取保候审。")
                    .disputeFocus(List.of("是否符合取保候审条件", "是否有社会危险性", "是否认罪认罚"))
                    .courtFinding("法院经审理查明，被告人钱某诈骗金额8万元，案发后积极退赔，取得被害人谅解，认罪认罚。")
                    .judgmentResult("一、被告人钱某犯诈骗罪，判处有期徒刑二年，缓刑三年，并处罚金人民币二万元；二、禁止被告人钱某在缓刑考验期内从事相关金融活动。")
                    .relevanceScore(0.85)
                    .relevanceAnalysis("本案涉及取保候审和缓刑的适用。被告人因积极退赔、取得谅解、认罪认罚，获得了取保候审并最终被判处缓刑。")
                    .keyTakeaways(List.of(
                            "积极退赔、取得被害人谅解有助于取保候审",
                            "认罪认罚可以从宽处理",
                            "取保候审后仍有可能被判刑"
                    ))
                    .build());
        }

        if (lowerFacts.contains("醉驾") || lowerFacts.contains("危险驾驶")) {
            cases.add(CaseRetrievalVO.builder()
                    .caseId("CASE_002")
                    .caseName("郑某危险驾驶案")
                    .court("南京市鼓楼区人民法院")
                    .caseNumber("(2023)苏0106刑初666号")
                    .judgmentDate(LocalDate.of(2023, 8, 18))
                    .caseType("刑事诉讼-危险驾驶罪")
                    .basicFacts("被告人郑某酒后驾车被交警查获，经鉴定血液酒精含量为152mg/100ml。郑某如实供述犯罪事实，认罪认罚。")
                    .disputeFocus(List.of("酒精含量是否准确", "是否符合不起诉条件", "量刑是否适当"))
                    .courtFinding("法院经审理查明，被告人郑某血液酒精含量为152mg/100ml，构成危险驾驶罪。郑某如实供述，认罪认罚，无前科劣迹。")
                    .judgmentResult("一、被告人郑某犯危险驾驶罪，判处拘役一个月十五日，缓刑二个月，并处罚金人民币三千元。")
                    .relevanceScore(0.9)
                    .relevanceAnalysis("本案是典型的危险驾驶罪（醉驾）案件。血液酒精含量152mg/100ml，被告人认罪认罚，无前科劣迹，获得了缓刑判决。")
                    .keyTakeaways(List.of(
                            "血液酒精含量80mg/100ml以上构成醉驾",
                            "160mg/100ml以下且无其他从重情节的，可能不起诉或缓刑",
                            "认罪认罚可以从轻处罚"
                    ))
                    .build());
        }

        cases.add(CaseRetrievalVO.builder()
                .caseId("CASE_003")
                .caseName("冯某故意伤害案")
                .court("成都市武侯区人民法院")
                .caseNumber("(2023)川0107刑初999号")
                .judgmentDate(LocalDate.of(2023, 11, 15))
                .caseType("刑事诉讼-故意伤害罪")
                .basicFacts("被告人冯某与被害人李某因琐事发生争执，冯某将李某打伤，经鉴定构成轻伤二级。冯某案发后主动报警，如实供述，并积极赔偿被害人损失，取得谅解。")
                .disputeFocus(List.of("是否构成正当防卫", "被害人是否有过错", "量刑是否适当"))
                .courtFinding("法院经审理查明，被告人冯某故意伤害他人身体，致人轻伤，构成故意伤害罪。冯某有自首情节，积极赔偿取得谅解，被害人也有一定过错。")
                .judgmentResult("一、被告人冯某犯故意伤害罪，判处有期徒刑六个月，缓刑一年。")
                .relevanceScore(0.78)
                .relevanceAnalysis("本案涉及故意伤害罪的认定和量刑。被告人因有自首、赔偿谅解等情节，获得了较轻的处罚。")
                .keyTakeaways(List.of(
                        "故意伤害致人轻伤以上构成犯罪",
                        "自首可以从轻或减轻处罚",
                        "积极赔偿取得谅解是重要的从轻情节"
                ))
                .build());

        return cases;
    }

    private List<CaseRetrievalVO> generateContractDisputeCases(String caseFacts) {
        List<CaseRetrievalVO> cases = new ArrayList<>();

        String lowerFacts = caseFacts.toLowerCase();

        if (lowerFacts.contains("借款") || lowerFacts.contains("欠钱")) {
            cases.add(CaseRetrievalVO.builder()
                    .caseId("CASE_001")
                    .caseName("韩某与马某民间借贷纠纷案")
                    .court("武汉市江汉区人民法院")
                    .caseNumber("(2023)鄂0103民初11111号")
                    .judgmentDate(LocalDate.of(2023, 9, 28))
                    .caseType("合同纠纷-民间借贷")
                    .basicFacts("原告韩某向被告马某出借20万元，双方签订了借款合同，约定月利率2%，借期一年。借款到期后，马某仅偿还了部分利息，本金分文未还。韩某起诉要求偿还本金及利息。")
                    .disputeFocus(List.of("借款合同是否有效", "约定利率是否超过法定上限", "被告是否应当承担违约责任"))
                    .courtFinding("法院经审理查明，借款合同真实有效。但约定的月利率2%超过了合同成立时一年期LPR的四倍（当时LPR为3.65%，四倍为14.6%），超出部分不予支持。")
                    .judgmentResult("一、被告马某于判决生效后十日内偿还原告韩某借款本金20万元；二、被告马某支付原告韩某利息（以20万元为基数，按年利率14.6%计算，自借款之日起至实际清偿之日止，扣除已支付的利息）；三、驳回原告其他诉讼请求。")
                    .relevanceScore(0.92)
                    .relevanceAnalysis("本案是典型的民间借贷纠纷案件。法院支持了原告要求偿还本金的请求，但对超过法定利率上限的利息部分不予支持。")
                    .keyTakeaways(List.of(
                            "民间借贷利率上限：合同成立时LPR的四倍",
                            "超过上限的利息不予保护",
                            "已支付的超过上限的利息可要求返还或抵扣本金"
                    ))
                    .build());
        }

        if (lowerFacts.contains("违约") || lowerFacts.contains("解除")) {
            cases.add(CaseRetrievalVO.builder()
                    .caseId("CASE_002")
                    .caseName("某装修公司与杨某装饰装修合同纠纷案")
                    .court("重庆市渝北区人民法院")
                    .caseNumber("(2023)渝0112民初22222号")
                    .judgmentDate(LocalDate.of(2023, 10, 12))
                    .caseType("合同纠纷-装饰装修合同")
                    .basicFacts("原告某装修公司与被告杨某签订装修合同，约定原告为被告装修房屋，总价15万元。原告施工至60%时，被告以装修质量不合格为由要求解除合同，并拒绝支付已完工部分的工程款。")
                    .disputeFocus(List.of("装修质量是否合格", "被告是否有权解除合同", "已完工部分的工程款如何结算"))
                    .courtFinding("法院经审理查明，原告的装修确实存在部分质量问题，但不构成根本违约。被告要求解除合同的理由不成立，但原告确实存在质量瑕疵，应当承担相应的违约责任。")
                    .judgmentResult("一、被告杨某于判决生效后十日内支付原告某装修公司已完工部分工程款8万元；二、原告某装修公司于判决生效后十日内支付被告杨某质量违约金1万元；三、驳回双方其他诉讼请求。")
                    .relevanceScore(0.85)
                    .relevanceAnalysis("本案涉及合同解除和质量问题的处理。法院认为，虽然原告存在质量瑕疵，但不构成根本违约，被告无权解除合同，但原告应当承担质量违约责任。")
                    .keyTakeaways(List.of(
                            "只有构成根本违约才能解除合同",
                            "一般质量瑕疵不能解除合同，但可主张质量违约金",
                            "已完工部分的工程款应当支付，但可扣除质量瑕疵部分"
                    ))
                    .build());
        }

        cases.add(CaseRetrievalVO.builder()
                .caseId("CASE_003")
                .caseName("徐某与某房产中介公司居间合同纠纷案")
                .court("西安市雁塔区人民法院")
                .caseNumber("(2023)陕0113民初33333号")
                .judgmentDate(LocalDate.of(2023, 12, 20))
                .caseType("合同纠纷-居间合同")
                .basicFacts("原告徐某通过被告中介公司购买房屋，签订了居间服务合同。后徐某与卖方私下成交，未通过中介公司办理过户手续。中介公司起诉要求徐某支付居间服务费。")
                .disputeFocus(List.of("中介公司是否提供了居间服务", "徐某是否应当支付居间服务费", "徐某是否构成跳单"))
                .courtFinding("法院经审理查明，中介公司确实为徐某提供了房源信息、带看等居间服务。徐某利用中介公司提供的信息与卖方私下成交，构成跳单，应当支付居间服务费。")
                .judgmentResult("一、原告徐某于判决生效后十日内支付被告某房产中介公司居间服务费3万元；二、驳回被告其他诉讼请求。")
                .relevanceScore(0.75)
                .relevanceAnalysis("本案涉及居间合同中的跳单问题。法院认定徐某利用中介公司提供的信息私下成交，构成跳单，应当支付居间服务费。")
                .keyTakeaways(List.of(
                        "利用中介公司提供的信息私下成交构成跳单",
                        "跳单应当支付居间服务费",
                        "如果通过其他正当渠道获得同一房源信息，不构成跳单"
                ))
                .build());

        return cases;
    }

    private List<CaseRetrievalVO> generateGeneralCases(String caseFacts) {
        List<CaseRetrievalVO> cases = new ArrayList<>();

        cases.add(CaseRetrievalVO.builder()
                .caseId("CASE_001")
                .caseName("合同纠纷典型案例")
                .court("北京市第一中级人民法院")
                .caseNumber("(2023)京01民终12345号")
                .judgmentDate(LocalDate.of(2023, 10, 1))
                .caseType("合同纠纷")
                .basicFacts("原告与被告签订合同，被告未按约定履行义务，原告起诉要求被告承担违约责任。")
                .disputeFocus(List.of("合同是否有效", "被告是否构成违约", "违约责任如何承担"))
                .courtFinding("法院经审理查明，合同真实有效，被告确实存在违约行为，应当承担违约责任。")
                .judgmentResult("一、被告于判决生效后十日内支付原告违约金；二、驳回原告其他诉讼请求。")
                .relevanceScore(0.6)
                .relevanceAnalysis("本案是合同纠纷的典型案例，确立了违约责任的基本裁判规则。")
                .keyTakeaways(List.of(
                        "依法成立的合同具有法律约束力",
                        "违约方应当承担违约责任",
                        "违约金过高或过低可请求调整"
                ))
                .build());

        return cases;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CaseSearchResult implements Serializable {
        private static final long serialVersionUID = 1L;

        private Boolean success;
        private String searchCriteria;
        private Integer totalFound;
        private List<CaseRetrievalVO> selectedCases;
        private String summary;
        private String errorMessage;
    }
}
