package com.agenticledger.agents.reconciliation;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.agenticledger.agents.mcp.SettlementDatabaseMCP;
import com.agenticledger.model.Settlement;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReconciliationOrchestrator {
    private static final Logger logger = LoggerFactory.getLogger(ReconciliationOrchestrator.class);

    private final Firestore firestore;
    private final SettlementDatabaseMCP mcp;
    private final ChatLanguageModel model;

    // Agent instances
    private final MatcherAgent matcherAgent;
    private final ValidatorAgent validatorAgent;
    private final ResolverAgent resolverAgent;

    /**
     * Constructor - Initialize orchestrator with all components
     */
    public ReconciliationOrchestrator(Firestore firestore, ChatLanguageModel model) {
        this.firestore = firestore;
        this.mcp = new SettlementDatabaseMCP(firestore);
        this.model = model;

        // Initialize agents
        this.matcherAgent = new MatcherAgent(firestore, mcp, model);
        this.validatorAgent = new ValidatorAgent(firestore, mcp, model);
        this.resolverAgent = new ResolverAgent(firestore, mcp, model);

        logger.info("🎯 ReconciliationOrchestrator initialized with all agents");
    }

    /**
     * Main orchestration method - Process a single settlement
     * Pipeline: Matcher → Validator → Resolver
     */
    public Map<String, Object> reconcileSettlement(String settlementId) throws Exception {
        logger.info("\n" + "=".repeat(80));
        logger.info("🚀 Starting reconciliation for settlement: {}", settlementId);
        logger.info("=".repeat(80));

        // Step 1: Get settlement
        Settlement settlement = mcp.getSettlement(settlementId);
        if (settlement == null) {
            logger.error("❌ Settlement not found: {}", settlementId);
            return createOrchestratorError(settlementId, "Settlement not found");
        }

        Map<String, Object> orchestrationResult = new HashMap<>();
        orchestrationResult.put("settlementId", settlementId);
        orchestrationResult.put("timestamp", System.currentTimeMillis());

        try {
            // Step 2: Run MatcherAgent
            logger.info("\n📍 STEP 1: MatcherAgent - Matching settlement with account");
            logger.info("-".repeat(80));
            Map<String, Object> matcherDecision = matcherAgent.matchSettlement(settlement);
            orchestrationResult.put("matcherDecision", matcherDecision);
            logger.info("✅ Matcher Decision: {}", matcherDecision.get("finalDecision"));

            // Step 3: Run ValidatorAgent
            logger.info("\n📍 STEP 2: ValidatorAgent - Validating against business rules");
            logger.info("-".repeat(80));
            Map<String, Object> validatorDecision = validatorAgent.validateSettlement(settlement, matcherDecision);
            orchestrationResult.put("validatorDecision", validatorDecision);
            logger.info("✅ Validator Decision: {} (Risk: {})",
                validatorDecision.get("validationStatus"),
                validatorDecision.get("riskScore"));

            // Step 4: Run ResolverAgent
            logger.info("\n📍 STEP 3: ResolverAgent - Resolving conflicts and making final decision");
            logger.info("-".repeat(80));
            Map<String, Object> resolverDecision = resolverAgent.resolveSettlement(
                settlement,
                matcherDecision,
                validatorDecision
            );
            orchestrationResult.put("resolverDecision", resolverDecision);
            logger.info("✅ Resolver Action: {}", resolverDecision.get("action"));

            // Step 5: Determine final outcome
            String finalAction = (String) resolverDecision.get("action");
            orchestrationResult.put("finalAction", finalAction);
            orchestrationResult.put("finalConfidence", resolverDecision.get("confidence"));

            // Step 6: Execute action (update database if approved)
            if ("AUTO_APPROVE".equals(finalAction)) {
                logger.info("\n💾 Updating settlement status to APPROVED");
                updateSettlementStatus(settlement, "APPROVED");
                orchestrationResult.put("status", "PROCESSED");
                orchestrationResult.put("outcome", "SETTLEMENT_PROCESSED_AND_APPROVED");
            } else if ("MANUAL_REVIEW".equals(finalAction)) {
                logger.info("\n👤 Settlement flagged for manual review");
                updateSettlementStatus(settlement, "PENDING_REVIEW");
                orchestrationResult.put("status", "PENDING_REVIEW");
                orchestrationResult.put("outcome", "REQUIRES_MANUAL_REVIEW");
            } else if ("REJECT".equals(finalAction)) {
                logger.info("\n❌ Settlement rejected - not processing");
                updateSettlementStatus(settlement, "REJECTED");
                orchestrationResult.put("status", "REJECTED");
                orchestrationResult.put("outcome", "SETTLEMENT_REJECTED");
            } else {
                logger.info("\n⏸️ Settlement held pending clarification");
                updateSettlementStatus(settlement, "HELD");
                orchestrationResult.put("status", "HELD");
                orchestrationResult.put("outcome", "SETTLEMENT_ON_HOLD");
            }

            logger.info("\n" + "=".repeat(80));
            logger.info("✅ RECONCILIATION COMPLETE - Settlement: {} - Action: {}",
                settlementId,
                finalAction);
            logger.info("=".repeat(80) + "\n");

            return orchestrationResult;

        } catch (Exception e) {
            logger.error("❌ Error during reconciliation: {}", e.getMessage(), e);
            orchestrationResult.put("status", "ERROR");
            orchestrationResult.put("error", e.getMessage());
            return orchestrationResult;
        }
    }

    /**
     * Batch reconciliation - Process multiple settlements
     */
    public Map<String, Object> reconcileMultipleSettlements(List<String> settlementIds) throws Exception {
        logger.info("\n🔄 Starting batch reconciliation for {} settlements", settlementIds.size());

        Map<String, Object> batchResult = new HashMap<>();
        Map<String, Object> results = new HashMap<>();

        int approved = 0;
        int rejected = 0;
        int pendingReview = 0;
        int errors = 0;

        for (String settlementId : settlementIds) {
            try {
                Map<String, Object> result = reconcileSettlement(settlementId);
                results.put(settlementId, result);

                String status = (String) result.get("status");
                switch (status) {
                    case "PROCESSED":
                        approved++;
                        break;
                    case "REJECTED":
                        rejected++;
                        break;
                    case "PENDING_REVIEW":
                        pendingReview++;
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                logger.error("Error reconciling settlement {}: {}", settlementId, e.getMessage());
                errors++;
                results.put(settlementId, Map.of("status", "ERROR", "error", e.getMessage()));
            }
        }

        batchResult.put("totalSettlements", settlementIds.size());
        batchResult.put("approved", approved);
        batchResult.put("rejected", rejected);
        batchResult.put("pendingReview", pendingReview);
        batchResult.put("errors", errors);
        batchResult.put("details", results);

        logger.info("\n📊 Batch Summary: {} approved, {} rejected, {} pending review, {} errors",
            approved, rejected, pendingReview, errors);

        return batchResult;
    }

    /**
     * Update settlement status in Firestore
     */
    private void updateSettlementStatus(Settlement settlement, String newStatus) throws Exception {
        settlement.setStatus(newStatus);
        firestore.collection("settlements")
            .document(settlement.getSettlementId())
            .set(settlement)
            .get();
        logger.info("Settlement {} status updated to {}", settlement.getSettlementId(), newStatus);
    }

    /**
     * Get orchestration stats
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("matcherAgent", matcherAgent.toString());
        stats.put("validatorAgent", validatorAgent.toString());
        stats.put("resolverAgent", resolverAgent.toString());
        stats.put("timestamp", System.currentTimeMillis());
        return stats;
    }

    private Map<String, Object> createOrchestratorError(String settlementId, String reason) {
        Map<String, Object> result = new HashMap<>();
        result.put("settlementId", settlementId);
        result.put("status", "ERROR");
        result.put("reason", reason);
        return result;
    }

    @Override
    public String toString() {
        return "ReconciliationOrchestrator{" +
                "matcherAgent=" + matcherAgent +
                ", validatorAgent=" + validatorAgent +
                ", resolverAgent=" + resolverAgent +
                '}';
    }

    /**
     * Main method for testing
     */
    public static void main(String[] args) throws Exception {
        logger.info("🎯 Starting ReconciliationOrchestrator");

        // Initialize Firestore (use emulator for development)
        System.setProperty("FIRESTORE_EMULATOR_HOST", "localhost:8080");
        Firestore firestore = FirestoreOptions.getDefaultInstance().getService();

        // Initialize Claude model
        String apiKey = System.getenv("ANTHROPIC_API_KEY");
        if (apiKey == null) {
            logger.error("ANTHROPIC_API_KEY environment variable not set");
            return;
        }

        ChatLanguageModel model = AnthropicChatModel.builder()
            .apiKey(apiKey)
            .modelName("claude-3-5-sonnet-20241022")
            .build();

        // Create orchestrator
        ReconciliationOrchestrator orchestrator = new ReconciliationOrchestrator(firestore, model);

        // Process sample settlements
        try {
            // Single settlement reconciliation
            Map<String, Object> result1 = orchestrator.reconcileSettlement("SETTLE-001");
            logger.info("Result for SETTLE-001: {}", result1);

            // Batch reconciliation
            List<String> settlementIds = List.of("SETTLE-001", "SETTLE-002");
            Map<String, Object> batchResult = orchestrator.reconcileMultipleSettlements(settlementIds);
            logger.info("Batch result: {}", batchResult);

        } catch (Exception e) {
            logger.error("Error: {}", e.getMessage(), e);
        } finally {
            firestore.close();
        }
    }
}
