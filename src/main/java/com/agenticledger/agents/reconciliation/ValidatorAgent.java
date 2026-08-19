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

public class ValidatorAgent {
    private static final Logger logger = LoggerFactory.getLogger(ValidatorAgent.class);

    private final Firestore firestore;
    private final SettlementDatabaseMCP mcp;
    private final ChatLanguageModel model;

    public ValidatorAgent(Firestore firestore, SettlementDatabaseMCP mcp, ChatLanguageModel model) {
        this.firestore = firestore;
        this.mcp = mcp;
        this.model = model;
        logger.info("✅ ValidatorAgent initialized");
    }

    /**
     * Validate settlement with business rules and compliance checks
     * Uses adversarial pattern: Validator vs Skeptic
     */
    public Map<String, Object> validateSettlement(Settlement settlement, Map<String, Object> matcherDecision) throws Exception {
        logger.info("🔍 Validating settlement: {}", settlement.getSettlementId());

        // Step 1: Get account for compliance checks
        Account account = mcp.getAccountPosition(settlement.getAccountId());
        if (account == null) {
            logger.error("Account not found for validation: {}", settlement.getAccountId());
            return createValidationFailure(settlement.getSettlementId(), "Account not found");
        }

        // Step 2: Role 1 - Validator checks business rules
        String validatorAnalysis = analyzeAsValidator(settlement, account, matcherDecision);
        logger.info("Validator analysis: {}", validatorAnalysis);

        // Step 3: Role 2 - Skeptic challenges the validation
        String skepticAnalysis = analyzeAsSkeptic(settlement, account, validatorAnalysis);
        logger.info("Skeptic analysis: {}", skepticAnalysis);

        // Step 4: Make validation decision
        Map<String, Object> decision = makeValidationDecision(
            settlement,
            account,
            validatorAnalysis,
            skepticAnalysis,
            matcherDecision
        );

        logger.info("✅ Settlement validated: {} with decision: {}",
            settlement.getSettlementId(),
            decision.get("validationStatus"));

        return decision;
    }

    /**
     * Role 1: Validator - Checks business rules and compliance
     */
    private String analyzeAsValidator(Settlement settlement, Account account, Map<String, Object> matcherDecision) {
        String prompt = PromptLibrary.getValidatorPrompt(settlement, account, matcherDecision);
        return model.generate(prompt);
    }

    /**
     * Role 2: Skeptic - Challenges the validation
     */
    private String analyzeAsSkeptic(Settlement settlement, Account account, String validatorAnalysis) {
        String prompt = PromptLibrary.getSkepticPrompt(settlement, account, validatorAnalysis);
        return model.generate(prompt);
    }

    /**
     * Make final validation decision
     */
    private Map<String, Object> makeValidationDecision(
        Settlement settlement,
        Account account,
        String validatorAnalysis,
        String skepticAnalysis,
        Map<String, Object> matcherDecision) {

        String arbitratorPrompt = PromptLibrary.getValidatorArbiterPrompt(
            settlement, account, validatorAnalysis, skepticAnalysis, matcherDecision
        );
        String decision = model.generate(arbitratorPrompt);

        // Parse and build response
        Map<String, Object> result = new HashMap<>();
        result.put("settlementId", settlement.getSettlementId());
        result.put("accountId", account.getAccountId());
        result.put("validatorAnalysis", validatorAnalysis);
        result.put("skepticAnalysis", skepticAnalysis);
        result.put("arbitratorDecision", decision);
        result.put("matcherDecision", matcherDecision.get("finalDecision"));

        if (decision.contains("PASS")) {
            result.put("validationStatus", "PASS");
            result.put("riskScore", 0.2);
        } else {
            result.put("validationStatus", "FAIL");
            result.put("riskScore", 0.8);
        }

        return result;
    }

    private Map<String, Object> createValidationFailure(String settlementId, String reason) {
        Map<String, Object> result = new HashMap<>();
        result.put("settlementId", settlementId);
        result.put("validationStatus", "ERROR");
        result.put("reason", reason);
        result.put("riskScore", 1.0);
        return result;
    }

    @Override
    public String toString() {
        return "ValidatorAgent{" +
                "firestore=" + firestore +
                ", mcp=" + mcp +
                '}';
    }
}
