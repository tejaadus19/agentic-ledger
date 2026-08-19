package com.agenticledger.agents.reconciliation;

import com.google.cloud.firestore.Firestore;
import com.agenticledger.agents.mcp.SettlementDatabaseMCP;
import com.agenticledger.agents.prompts.PromptLibrary;
import com.agenticledger.model.Settlement;
import com.agenticledger.model.Account;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ResolverAgent {
    private static final Logger logger = LoggerFactory.getLogger(ResolverAgent.class);

    private final Firestore firestore;
    private final SettlementDatabaseMCP mcp;
    private final ChatLanguageModel model;

    public ResolverAgent(Firestore firestore, SettlementDatabaseMCP mcp, ChatLanguageModel model) {
        this.firestore = firestore;
        this.mcp = mcp;
        this.model = model;
        logger.info("✅ ResolverAgent initialized");
    }

    /**
     * Resolve settlement conflicts/mismatches
     * Uses pattern: Resolver vs Conservative
     */
    public Map<String, Object> resolveSettlement(
        Settlement settlement,
        Map<String, Object> matcherDecision,
        Map<String, Object> validatorDecision) throws Exception {

        logger.info("⚙️ Resolving settlement: {}", settlement.getSettlementId());

        // Get account for context
        Account account = mcp.getAccountPosition(settlement.getAccountId());
        if (account == null) {
            logger.error("Account not found for resolution: {}", settlement.getAccountId());
            return createResolutionFailure(settlement.getSettlementId(), "Account not found");
        }

        // Check if resolution is even needed
        String matcherStatus = (String) matcherDecision.get("finalDecision");
        String validatorStatus = (String) validatorDecision.get("validationStatus");

        if ("APPROVE".equals(matcherStatus) && "PASS".equals(validatorStatus)) {
            logger.info("✅ No resolution needed - settlement approved and validated");
            return createAutoApprovalDecision(settlement, account, matcherDecision, validatorDecision);
        }

        // Step 1: Resolver analyzes conflict
        String resolverAnalysis = analyzeAsResolver(settlement, account, matcherDecision, validatorDecision);
        logger.info("Resolver analysis: {}", resolverAnalysis);

        // Step 2: Conservative challenges resolution
        String conservativeAnalysis = analyzeAsConservative(settlement, account, resolverAnalysis);
        logger.info("Conservative analysis: {}", conservativeAnalysis);

        // Step 3: Make resolution decision
        Map<String, Object> decision = makeResolutionDecision(
            settlement,
            account,
            resolverAnalysis,
            conservativeAnalysis,
            matcherDecision,
            validatorDecision
        );

        logger.info("✅ Settlement resolved: {} with action: {}",
            settlement.getSettlementId(),
            decision.get("action"));

        return decision;
    }

    /**
     * Role 1: Resolver - Proposes resolution strategies
     */
    private String analyzeAsResolver(
        Settlement settlement,
        Account account,
        Map<String, Object> matcherDecision,
        Map<String, Object> validatorDecision) {

        String prompt = PromptLibrary.getResolverPrompt(settlement, account, matcherDecision, validatorDecision);
        return model.generate(prompt);
    }

    /**
     * Role 2: Conservative - Challenges the resolver's decision
     */
    private String analyzeAsConservative(Settlement settlement, Account account, String resolverAnalysis) {
        String prompt = PromptLibrary.getConservativePrompt(settlement, account, resolverAnalysis);
        return model.generate(prompt);
    }

    /**
     * Make final resolution decision
     */
    private Map<String, Object> makeResolutionDecision(
        Settlement settlement,
        Account account,
        String resolverAnalysis,
        String conservativeAnalysis,
        Map<String, Object> matcherDecision,
        Map<String, Object> validatorDecision) {

        String arbitratorPrompt = PromptLibrary.getResolverArbiterPrompt(
            settlement, account, resolverAnalysis, conservativeAnalysis, matcherDecision, validatorDecision
        );
        String decision = model.generate(arbitratorPrompt);

        // Parse and build response
        Map<String, Object> result = new HashMap<>();
        result.put("settlementId", settlement.getSettlementId());
        result.put("accountId", account.getAccountId());
        result.put("resolverAnalysis", resolverAnalysis);
        result.put("conservativeAnalysis", conservativeAnalysis);
        result.put("arbitratorDecision", decision);
        result.put("matcherDecision", matcherDecision.get("finalDecision"));
        result.put("validatorDecision", validatorDecision.get("validationStatus"));
        result.put("riskScore", validatorDecision.get("riskScore"));

        // Extract action
        if (decision.contains("AUTO_APPROVE")) {
            result.put("action", "AUTO_APPROVE");
            result.put("confidence", 0.95);
        } else if (decision.contains("MANUAL_REVIEW")) {
            result.put("action", "MANUAL_REVIEW");
            result.put("confidence", 0.7);
        } else if (decision.contains("REJECT")) {
            result.put("action", "REJECT");
            result.put("confidence", 0.85);
        } else if (decision.contains("HOLD")) {
            result.put("action", "HOLD");
            result.put("confidence", 0.6);
        } else {
            result.put("action", "PARTIAL_APPROVE");
            result.put("confidence", 0.5);
        }

        return result;
    }

    /**
     * Auto-approval decision when matcher + validator agree
     */
    private Map<String, Object> createAutoApprovalDecision(
        Settlement settlement,
        Account account,
        Map<String, Object> matcherDecision,
        Map<String, Object> validatorDecision) {

        Map<String, Object> result = new HashMap<>();
        result.put("settlementId", settlement.getSettlementId());
        result.put("accountId", account.getAccountId());
        result.put("action", "AUTO_APPROVE");
        result.put("confidence", 0.98);
        result.put("matcherDecision", matcherDecision.get("finalDecision"));
        result.put("validatorDecision", validatorDecision.get("validationStatus"));
        result.put("riskScore", 0.1);
        result.put("reason", "Settlement matched and validated - auto-approved");
        return result;
    }

    private Map<String, Object> createResolutionFailure(String settlementId, String reason) {
        Map<String, Object> result = new HashMap<>();
        result.put("settlementId", settlementId);
        result.put("action", "ERROR");
        result.put("reason", reason);
        result.put("confidence", 0.0);
        return result;
    }

    @Override
    public String toString() {
        return "ResolverAgent{" +
                "firestore=" + firestore +
                ", mcp=" + mcp +
                '}';
    }
}
