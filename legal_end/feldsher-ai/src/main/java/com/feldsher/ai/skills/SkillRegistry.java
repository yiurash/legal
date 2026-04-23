package com.feldsher.ai.skills;

import com.feldsher.ai.skills.child.ContractDisputeSkill;
import com.feldsher.ai.skills.child.CriminalProcedureSkill;
import com.feldsher.ai.skills.child.LaborDisputeSkill;
import com.feldsher.ai.skills.child.MarriageFamilySkill;
import com.feldsher.ai.skills.parent.CaseRetrievalParentSkill;
import com.feldsher.ai.skills.parent.FactCollectionParentSkill;
import com.feldsher.ai.skills.parent.LawRetrievalParentSkill;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SkillRegistry {

    private final FactCollectionParentSkill factCollectionParentSkill;
    private final LawRetrievalParentSkill lawRetrievalParentSkill;
    private final CaseRetrievalParentSkill caseRetrievalParentSkill;
    private final MarriageFamilySkill marriageFamilySkill;
    private final LaborDisputeSkill laborDisputeSkill;
    private final CriminalProcedureSkill criminalProcedureSkill;
    private final ContractDisputeSkill contractDisputeSkill;

    private final Map<String, LegalSkill> skillMap = new HashMap<>();

    @PostConstruct
    public void init() {
        registerSkill(factCollectionParentSkill);
        registerSkill(lawRetrievalParentSkill);
        registerSkill(caseRetrievalParentSkill);
        registerSkill(marriageFamilySkill);
        registerSkill(laborDisputeSkill);
        registerSkill(criminalProcedureSkill);
        registerSkill(contractDisputeSkill);

        log.info("Skill注册完成，共注册 {} 个Skill", skillMap.size());
    }

    private void registerSkill(LegalSkill skill) {
        skillMap.put(skill.getSkillCode(), skill);
        log.info("注册Skill: {} - {}", skill.getSkillCode(), skill.getSkillName());
    }

    public LegalSkill getSkill(String skillCode) {
        return skillMap.get(skillCode);
    }

    public LegalSkill getSkillByCaseType(String caseTypeCode) {
        return switch (caseTypeCode) {
            case "marriage_family" -> marriageFamilySkill;
            case "labor_dispute" -> laborDisputeSkill;
            case "criminal_procedure" -> criminalProcedureSkill;
            case "contract_dispute" -> contractDisputeSkill;
            default -> factCollectionParentSkill;
        };
    }

    public boolean hasSkill(String skillCode) {
        return skillMap.containsKey(skillCode);
    }
}
