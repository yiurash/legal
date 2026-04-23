package com.feldsher.ai.agents;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgentRegistry {

    private final MasterAgent masterAgent;
    private final FactCollectionAgent factCollectionAgent;
    private final LawRetrievalAgent lawRetrievalAgent;
    private final CaseRetrievalAgent caseRetrievalAgent;

    private final Map<String, LegalAgent> agentMap = new HashMap<>();

    @PostConstruct
    public void init() {
        registerAgent(masterAgent);
        registerAgent(factCollectionAgent);
        registerAgent(lawRetrievalAgent);
        registerAgent(caseRetrievalAgent);

        log.info("Agent注册完成，共注册 {} 个Agent", agentMap.size());
    }

    private void registerAgent(LegalAgent agent) {
        agentMap.put(agent.getAgentCode(), agent);
        log.info("注册Agent: {} - {}", agent.getAgentCode(), agent.getAgentName());
    }

    public LegalAgent getAgent(String agentCode) {
        return agentMap.get(agentCode);
    }

    public MasterAgent getMasterAgent() {
        return masterAgent;
    }

    public FactCollectionAgent getFactCollectionAgent() {
        return factCollectionAgent;
    }

    public LawRetrievalAgent getLawRetrievalAgent() {
        return lawRetrievalAgent;
    }

    public CaseRetrievalAgent getCaseRetrievalAgent() {
        return caseRetrievalAgent;
    }

    public boolean hasAgent(String agentCode) {
        return agentMap.containsKey(agentCode);
    }
}
