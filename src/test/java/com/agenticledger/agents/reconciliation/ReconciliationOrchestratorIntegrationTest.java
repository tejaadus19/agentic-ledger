package com.agenticledger.agents.reconciliation;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.agenticledger.model.Settlement;
import com.agenticledger.model.Account;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Disabled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration Tests - Uses REAL Claude API and Firestore Emulator
 * Tag: @Tag("integration") - Run separately from unit tests
 * Run with: mvn test -Dgroups=integration
 * Or: mvn test -Dtest=ReconciliationOrchestratorIntegrationTest
 */
@Tag("integration")
@DisplayName("ReconciliationOrchestrator REAL Integration Tests")
class ReconciliationOrchestratorIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(ReconciliationOrchestratorIntegrationTest.class);

    private static Firestore firestore;
    private static ChatLanguageModel realClaudeModel;
    private ReconciliationOrchestrator orchestrator;

    /**
     * One-time setup - Initialize REAL Claude API and Firestore
     */
    @BeforeAll
    static void setupRealDependencies() {
        logger.info("\n" + "=".repeat(80));
        logger.info("🚀 INTEGRATION TEST SETUP - Initializing REAL Claude API");
        logger.info("=".repeat(80));

        // Setup Firestore Emulator
        System.setProperty("FIRESTORE_EMULATOR_HOST", "localhost:8080");
        firestore = FirestoreOptions.getDefaultInstance().getService();
        logger.info("✅ Firestore Emulator connected on localhost:8080");

        // Setup REAL Claude API
        String apiKey = System.getenv("ANTHROPIC_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            logger.error("❌ ANTHROPIC_API_KEY environment variable not set!");
            logger.error("   Set it with: export ANTHROPIC_API_KEY=sk-...");
            throw new IllegalStateException("ANTHROPIC_API_KEY not configured");
        }

        realClaudeModel = AnthropicChatModel.builder()
            .apiKey(apiKey)
            .modelName("claude-3-5-sonnet-20241022")
            .maxTokens(1000)
            .build();

        logger.info("✅ Claude API (claude-3-5-sonnet-20241022) initialized");
        logger.info("=".repeat(80) + "\n");
    }

    /**
     * Per-test setup - Initialize orchestrator with REAL components
     */
    @BeforeEach
    void setupPerTest() throws Exception {
        orchestrator = new ReconciliationOrchestrator(firestore, realClaudeModel);
        loadSampleData();
        logger.info("✅ Orchestrator ready with REAL Claude API and sample data\n");
    }

    /**
     * Load sample data to Firestore for testing
     */
    private void loadSampleData() throws Exception {
        logger.info("📝 Loading sample data to Firestore...");

        // Create sample accounts
        Map<String, Object> account1 = new HashMap<>();
        account1.put("accountId", "ACC-001");
        account1.put("accountName", "Test Trust Account A");
        account1.put("balance", 150000.0);
        account1.put("currency", "USD");
        account1.put("status", "ACTIVE");

        firestore.collection("accounts").document("ACC-001").set(account1).get();
        logger.info("  ✅ Created ACC-001");

        // Create sample settlements
        Map<String, Object> settlement1 = new HashMap<>();
        settlement1.put("settlementId", "SETTLE-001");
        settlement1.put("accountId", "ACC-001");
        settlement1.put("security", "AAPL");
        settlement1.put("quantity", 100.0);
        settlement1.put("amount", 15050.0);
        settlement1.put("currency", "USD");
        settlement1.put("status", "NEW");

        firestore.collection("settlements").document("SETTLE-001").set(settlement1).get();
        logger.info("  ✅ Created SETTLE-001");

        Map<String, Object> settlement2 = new HashMap<>();
        settlement2.put("settlementId", "SETTLE-002");
        settlement2.put("accountId", "ACC-001");
        settlement2.put("security", "MSFT");
        settlement2.put("quantity", 50.0);
        settlement2.put("amount", 17500.0);
        settlement2.put("currency", "USD");
        settlement2.put("status", "NEW");

        firestore.collection("settlements").document("SETTLE-002").set(settlement2).get();
        logger.info("  ✅ Created SETTLE-002\n");
    }

    /**
     * TEST 1: Real-world settlement reconciliation end-to-end
     * This ACTUALLY calls Claude API!
     */
    @Test
    @DisplayName("Should reconcile settlement with REAL Claude API reasoning")
    void testRealSettlementReconciliation() throws Exception {
        logger.info("\n" + "─".repeat(80));
        logger.info("🧪 TEST 1: Real Settlement Reconciliation with Claude");
        logger.info("─".repeat(80));

        // This will ACTUALLY call Claude API with real reasoning
        Map<String, Object> result = orchestrator.reconcileSettlement("SETTLE-001");

        // Verify result structure
        assertNotNull(result, "Result should not be null");
        assertEquals("SETTLE-001", result.get("settlementId"), "Settlement ID should match");

        // Verify all agent decisions are present
        assertNotNull(result.get("matcherDecision"), "Matcher decision should be present");
        assertNotNull(result.get("validatorDecision"), "Validator decision should be present");
        assertNotNull(result.get("resolverDecision"), "Resolver decision should be present");

        // Verify final outcome
        assertNotNull(result.get("finalAction"), "Final action should be present");
        assertNotNull(result.get("status"), "Status should be present");

        logger.info("\n📊 Real Reconciliation Result:");
        logger.info("  Settlement: {}", result.get("settlementId"));
        logger.info("  Matcher: {}", ((Map<String, Object>) result.get("matcherDecision")).get("finalDecision"));
        logger.info("  Validator: {}", ((Map<String, Object>) result.get("validatorDecision")).get("validationStatus"));
        logger.info("  Final Action: {}", result.get("finalAction"));
        logger.info("  Status: {}", result.get("status"));

        // Verify Claude reasoning is present (not empty strings)
        Map<String, Object> matcherDecision = (Map<String, Object>) result.get("matcherDecision");
        assertNotNull(matcherDecision.get("matcherAnalysis"), "Matcher analysis should have Claude reasoning");
        assertTrue(((String) matcherDecision.get("matcherAnalysis")).length() > 10,
            "Matcher analysis should contain meaningful Claude output");

        logger.info("\n✅ TEST 1 PASSED: Real reconciliation completed successfully\n");
    }

    /**
     * TEST 2: Batch reconciliation with REAL Claude
     */
    @Test
    @DisplayName("Should batch reconcile multiple settlements with REAL Claude")
    void testRealBatchReconciliation() throws Exception {
        logger.info("\n" + "─".repeat(80));
        logger.info("🧪 TEST 2: Batch Reconciliation with Real Claude");
        logger.info("─".repeat(80));

        List<String> settlementIds = List.of("SETTLE-001", "SETTLE-002");

        Map<String, Object> batchResult = orchestrator.reconcileMultipleSettlements(settlementIds);

        assertNotNull(batchResult, "Batch result should not be null");
        assertEquals(2, batchResult.get("totalSettlements"), "Should process 2 settlements");

        int approved = (int) batchResult.get("approved");
        int rejected = (int) batchResult.get("rejected");
        int pendingReview = (int) batchResult.get("pendingReview");

        logger.info("\n📊 Real Batch Reconciliation Results:");
        logger.info("  Total Settlements: {}", batchResult.get("totalSettlements"));
        logger.info("  Approved: {}", approved);
        logger.info("  Rejected: {}", rejected);
        logger.info("  Pending Review: {}", pendingReview);

        int total = approved + rejected + pendingReview + (int) batchResult.get("errors");
        assertEquals(2, total, "All settlements should be processed");

        logger.info("\n✅ TEST 2 PASSED: Batch reconciliation completed successfully\n");
    }

    /**
     * TEST 3: Verify Claude reasoning quality
     */
    @Test
    @DisplayName("Should produce high-quality reasoning from Claude")
    void testClaudeReasoningQuality() throws Exception {
        logger.info("\n" + "─".repeat(80));
        logger.info("🧪 TEST 3: Verify Claude Reasoning Quality");
        logger.info("─".repeat(80));

        Map<String, Object> result = orchestrator.reconcileSettlement("SETTLE-001");

        Map<String, Object> matcherDecision = (Map<String, Object>) result.get("matcherDecision");
        String matcherAnalysis = (String) matcherDecision.get("matcherAnalysis");

        // Verify Claude output is substantive (not just "yes" or "no")
        assertTrue(matcherAnalysis.length() > 50, "Analysis should be detailed");

        // Verify key analysis components
        boolean hasAnalysis = matcherAnalysis.length() > 0;
        boolean hasConfidence = matcherDecision.get("confidence") != null;

        assertTrue(hasAnalysis, "Should have detailed analysis from Claude");
        assertTrue(hasConfidence, "Should have confidence score");

        logger.info("\n💡 Claude Analysis Sample:");
        logger.info(matcherAnalysis.substring(0, Math.min(200, matcherAnalysis.length())));
        logger.info("\n... [truncated for brevity]");

        logger.info("\n✅ TEST 3 PASSED: Claude reasoning is substantive\n");
    }

    /**
     * TEST 4: Test adversarial pattern in action
     */
    @Test
    @DisplayName("Should execute full adversarial pattern with REAL agents")
    void testAdversarialPatternWithRealClaude() throws Exception {
        logger.info("\n" + "─".repeat(80));
        logger.info("🧪 TEST 4: Adversarial Pattern Execution");
        logger.info("─".repeat(80));

        Map<String, Object> result = orchestrator.reconcileSettlement("SETTLE-001");

        // Get all analyses
        Map<String, Object> matcherDecision = (Map<String, Object>) result.get("matcherDecision");
        Map<String, Object> validatorDecision = (Map<String, Object>) result.get("validatorDecision");
        Map<String, Object> resolverDecision = (Map<String, Object>) result.get("resolverDecision");

        // Verify all roles executed
        assertNotNull(matcherDecision.get("matcherAnalysis"), "Matcher should provide analysis");
        assertNotNull(matcherDecision.get("devilsAdvocateAnalysis"), "Devil's advocate should challenge");
        assertNotNull(validatorDecision.get("validatorAnalysis"), "Validator should check rules");
        assertNotNull(validatorDecision.get("skepticAnalysis"), "Skeptic should challenge");

        logger.info("\n🎭 Adversarial Pattern Results:");
        logger.info("  1️⃣  Matcher: {}", ((String) matcherDecision.get("matcherAnalysis")).substring(0, 50) + "...");
        logger.info("  2️⃣  Devil's Advocate: {}", ((String) matcherDecision.get("devilsAdvocateAnalysis")).substring(0, 50) + "...");
        logger.info("  3️⃣  Validator: {}", ((String) validatorDecision.get("validatorAnalysis")).substring(0, 50) + "...");
        logger.info("  4️⃣  Skeptic: {}", ((String) validatorDecision.get("skepticAnalysis")).substring(0, 50) + "...");
        logger.info("  5️⃣  Resolver: {}", resolverDecision.get("action"));

        logger.info("\n✅ TEST 4 PASSED: Full adversarial pipeline executed\n");
    }

    /**
     * TEST 5: Verify Firestore state updates
     */
    @Test
    @DisplayName("Should update settlement status in Firestore after reconciliation")
    void testFirestoreStateUpdate() throws Exception {
        logger.info("\n" + "─".repeat(80));
        logger.info("🧪 TEST 5: Firestore State Update Verification");
        logger.info("─".repeat(80));

        // Check initial state
        Settlement beforeSettlement = firestore.collection("settlements")
            .document("SETTLE-001")
            .get()
            .get()
            .toObject(Settlement.class);

        logger.info("  Before: {}", beforeSettlement.getStatus());
        assertEquals("NEW", beforeSettlement.getStatus(), "Initial status should be NEW");

        // Reconcile
        Map<String, Object> result = orchestrator.reconcileSettlement("SETTLE-001");

        // Check updated state
        Settlement afterSettlement = firestore.collection("settlements")
            .document("SETTLE-001")
            .get()
            .get()
            .toObject(Settlement.class);

        logger.info("  After: {}", afterSettlement.getStatus());

        // Status should be updated based on final action
        String finalAction = (String) result.get("finalAction");
        assertNotNull(afterSettlement.getStatus(), "Status should be updated");
        assertNotEquals("NEW", afterSettlement.getStatus(), "Status should change from NEW");

        logger.info("\n  ✅ Firestore state correctly updated");
        logger.info("✅ TEST 5 PASSED: State persistence verified\n");
    }

    /**
     * Performance test - measure Claude response time
     */
    @Test
    @DisplayName("Should complete reconciliation within reasonable time")
    void testReconciliationPerformance() throws Exception {
        logger.info("\n" + "─".repeat(80));
        logger.info("🧪 TEST 6: Performance Measurement");
        logger.info("─".repeat(80));

        long startTime = System.currentTimeMillis();

        Map<String, Object> result = orchestrator.reconcileSettlement("SETTLE-001");

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        logger.info("\n⏱️  Reconciliation took: {} ms ({} seconds)", duration, duration / 1000.0);
        logger.info("  Action: {}", result.get("finalAction"));

        // Verify it completes (Claude responses vary, so we just check it's not absurdly slow)
        assertTrue(duration < 120000, "Should complete within 2 minutes");

        logger.info("✅ TEST 6 PASSED: Performance acceptable\n");
    }

    /**
     * Summary report
     */
    @Test
    @DisplayName("Should generate orchestrator statistics")
    void testOrchestratorStats() throws Exception {
        logger.info("\n" + "─".repeat(80));
        logger.info("🧪 TEST 7: Orchestrator Statistics");
        logger.info("─".repeat(80));

        Map<String, Object> stats = orchestrator.getStats();

        assertNotNull(stats, "Stats should not be null");
        assertNotNull(stats.get("timestamp"), "Should have timestamp");

        logger.info("\n📊 Orchestrator Stats:");
        logger.info("  Timestamp: {}", stats.get("timestamp"));
        logger.info("  Components: {}", stats.keySet());

        logger.info("✅ TEST 7 PASSED\n");
    }

    /**
     * Cleanup hint
     */
    static void cleanup() throws Exception {
        if (firestore != null) {
            firestore.close();
            logger.info("✅ Firestore connection closed");
        }
    }
}
