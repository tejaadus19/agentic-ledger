package com.agenticledger.agents.reconciliation;

import com.google.cloud.firestore.Firestore;
import com.agenticledger.agents.mcp.SettlementDatabaseMCP;
import com.agenticledger.model.Settlement;
import com.agenticledger.model.Account;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class MatcherAgent {
    private static final Logger logger = LoggerFactory.getLogger(MatcherAgent.class);

    private final Firestore firestore;
    private final SettlementDatabaseMCP mcp;
    private final ChatLanguageModel model;

    /**
     * Constructor for MatcherAgent
     */
    public MatcherAgent(Firestore firestore, SettlementDatabaseMCP mcp, ChatLanguageModel model) {
        this.firestore = firestore;
        this.mcp = mcp;
        this.model = model;
        logger.info("✅ MatcherAgent initialized");
    }

    /**
     * Match settlement with account position
     * Uses adversarial pattern: Matcher vs Devil's Advocate
     */
    public Map<String, Object> matchSettlement(Settlement settlement) throws Exception {
        logger.info("🧪 Matching settlement: {}", settlement.getSettlementId());

        // Step 1: Get account position
        Account account = mcp.getAccountPosition(settlement.getAccountId());
        if (account == null) {
            logger.error("Account not found: {}", settlement.getAccountId());
            return createFailureResponse(settlement.getSettlementId(), "Account not found");
        }

        // Step 2: Role 1 - Matcher Agent analyzes
        String matcherAnalysis = analyzeAsMatchMaker(settlement, account);
        logger.info("Matcher analysis: {}", matcherAnalysis);

        // Step 3: Role 2 - Devil's Advocate challenges
        String devilsAdvocateAnalysis = analyzeAsDevilsAdvocate(settlement, account, matcherAnalysis);
        logger.info("Devil's advocate analysis: {}", devilsAdvocateAnalysis);

        // Step 4: Make final decision
        Map<String, Object> decision = makeArbiterDecision(
            settlement,
            account,
            matcherAnalysis,
            devilsAdvocateAnalysis
        );

        logger.info("✅ Settlement matched: {} with decision: {}",
            settlement.getSettlementId(),
            decision.get("finalDecision"));

        return decision;
    }

    /**
     * Role 1: Matcher Agent - Analyzes if settlement matches account position
     */
    private String analyzeAsMatchMaker(Settlement settlement, Account account) {
        String prompt = String.format(
            "You are a Matcher Agent analyzing a settlement reconciliation.\n\n" +
            "Settlement Details:\n" +
            "- ID: %s\n" +
            "- Security: %s\n" +
            "- Quantity: %f\n" +
            "- Amount: $%f\n" +
            "- Currency: %s\n\n" +
            "Account Details:\n" +
            "- ID: %s\n" +
            "- Name: %s\n" +
            "- Current Balance: $%f\n" +
            "- Status: %s\n\n" +
            "Question: Does this settlement MATCH the account position?\n" +
            "Provide analysis: Is the amount and security correct?\n" +
            "Answer with MATCH or MISMATCH and explain briefly.",

            settlement.getSettlementId(),
            settlement.getSecurity(),
            settlement.getQuantity(),
            settlement.getAmount(),
            settlement.getCurrency(),
            account.getAccountId(),
            account.getAccountName(),
            account.getBalance(),
            account.getStatus()
        );

        return model.generate(prompt);
    }

    /**
     * Role 2: Devil's Advocate - Challenges the matcher's analysis
     */
    private String analyzeAsDevilsAdvocate(Settlement settlement, Account account, String matcherAnalysis) {
        String prompt = String.format(
            "You are a Devil's Advocate Agent reviewing a settlement analysis.\n\n" +
            "Settlement: %s for $%f of %s\n" +
            "Account Balance: $%f\n\n" +
            "Matcher Agent's Analysis:\n%s\n\n" +
            "Your role: Challenge this analysis!\n" +
            "Ask critical questions:\n" +
            "- What if there's a duplicate settlement?\n" +
            "- What if the account is restricted?\n" +
            "- What if there's a timing issue?\n" +
            "- What edge cases might be missed?\n\n" +
            "Provide counter-arguments and risks.",

            settlement.getSettlementId(),
            settlement.getAmount(),
            settlement.getSecurity(),
            account.getBalance(),
            matcherAnalysis
        );

        return model.generate(prompt);
    }

    /**
     * Make final decision based on both analyses
     */
    private Map<String, Object> makeArbiterDecision(
        Settlement settlement,
        Account account,
        String matcherAnalysis,
        String devilsAdvocateAnalysis) {

        String arbitratorPrompt = String.format(
            "You are an Arbitrator Agent making the final decision on a settlement.\n\n" +
            "Settlement: %s, Amount: $%f\n" +
            "Account: %s, Balance: $%f\n\n" +
            "Matcher Agent says:\n%s\n\n" +
            "Devil's Advocate says:\n%s\n\n" +
            "Based on both perspectives, make a FINAL DECISION.\n" +
            "Respond in this format:\n" +
            "DECISION: [APPROVE or REJECT]\n" +
            "CONFIDENCE: [0.0 to 1.0]\n" +
            "REASONING: [one sentence]",

            settlement.getSettlementId(),
            settlement.getAmount(),
            account.getAccountId(),
            account.getBalance(),
            matcherAnalysis,
            devilsAdvocateAnalysis
        );

        String decision = model.generate(arbitratorPrompt);

        // Parse the response
        Map<String, Object> result = new HashMap<>();
        result.put("settlementId", settlement.getSettlementId());
        result.put("accountId", account.getAccountId());
        result.put("matcherAnalysis", matcherAnalysis);
        result.put("devilsAdvocateAnalysis", devilsAdvocateAnalysis);
        result.put("arbitratorDecision", decision);

        // Extract decision
        if (decision.contains("APPROVE")) {
            result.put("finalDecision", "APPROVE");
            result.put("confidence", 0.9);
        } else {
            result.put("finalDecision", "REJECT");
            result.put("confidence", 0.7);
        }

        return result;
    }

    /**
     * Create failure response
     */
    private Map<String, Object> createFailureResponse(String settlementId, String reason) {
        Map<String, Object> result = new HashMap<>();
        result.put("settlementId", settlementId);
        result.put("finalDecision", "ERROR");
        result.put("reason", reason);
        result.put("confidence", 0.0);
        return result;
    }

    @Override
    public String toString() {
        return "MatcherAgent{" +
                "firestore=" + firestore +
                ", mcp=" + mcp +
                '}';
    }
}
