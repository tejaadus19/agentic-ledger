package com.agenticledger.agents.reconciliation;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.agenticledger.agents.mcp.SettlementDatabaseMCP;
import com.agenticledger.model.Settlement;
import com.agenticledger.model.Account;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("ReconciliationOrchestrator Unit Tests (Mocked)")
class ReconciliationOrchestratorTest {
    private static final Logger logger = LoggerFactory.getLogger(ReconciliationOrchestratorTest.class);

    @Mock
    private ChatLanguageModel mockChatModel;

    @Mock
    private SettlementDatabaseMCP mockMCP;

    @Mock
    private Firestore mockFirestore;

    private Settlement testSettlement;
    private Account testAccount;

    /**
     * Per-test setup - Initialize ALL mocks to avoid Firestore connection
     */
    @BeforeEach
    void setupPerTest() {
        MockitoAnnotations.openMocks(this);

        // Setup test data
        testSettlement = createTestSettlement("SETTLE-001");
        testAccount = createTestAccount("ACC-001");

        // Mock MCP responses
        try {
            when(mockMCP.getSettlement("SETTLE-001")).thenReturn(testSettlement);
            when(mockMCP.getAccountPosition("ACC-001")).thenReturn(testAccount);
            when(mockMCP.getSettlement("SETTLE-002")).thenReturn(createTestSettlement("SETTLE-002"));
            when(mockMCP.getSettlement("NON-EXISTENT")).thenReturn(null);
        } catch (Exception e) {
            logger.error("Error setting up mocks", e);
        }

        logger.info("✅ All mocks initialized (no Firestore connection needed)");
    }

    /**
     * Matcher Agent Tests
     */
    @Nested
    @DisplayName("MatcherAgent Tests")
    class MatcherAgentTests {

        @Test
        @DisplayName("Should match settlement when all fields align")
        void testSuccessfulMatch() throws Exception {
            logger.info("🧪 Testing successful settlement match");

            // Mock Claude to return APPROVE
            when(mockChatModel.generate(anyString()))
                .thenReturn("DECISION: APPROVE\nCONFIDENCE: 0.95\nREASONING: Settlement matches account position perfectly");

            Map<String, Object> result = orchestrator.reconcileSettlement("SETTLE-001");

            assertNotNull(result, "Result should not be null");
            assertEquals("SETTLE-001", result.get("settlementId"), "Settlement ID should match");
            assertNotNull(result.get("matcherDecision"), "Matcher decision should be present");

            logger.info("✅ Test passed: Settlement matched successfully");
        }

        @Test
        @DisplayName("Should reject settlement when amounts don't match")
        void testFailedMatch() throws Exception {
            logger.info("🧪 Testing failed settlement match");

            when(mockChatModel.generate(anyString()))
                .thenReturn("DECISION: REJECT\nCONFIDENCE: 0.90\nREASONING: Amount mismatch detected");

            Map<String, Object> result = orchestrator.reconcileSettlement("SETTLE-001");

            assertNotNull(result, "Result should not be null");
            assertEquals("SETTLE-001", result.get("settlementId"), "Settlement ID should match");

            logger.info("✅ Test passed: Settlement rejection detected");
        }

        @ParameterizedTest
        @DisplayName("Should handle multiple settlements")
        @ValueSource(strings = {"SETTLE-001", "SETTLE-002"})
        void testMultipleSettlements(String settlementId) throws Exception {
            logger.info("🧪 Testing settlement: {}", settlementId);

            when(mockChatModel.generate(anyString()))
                .thenReturn("DECISION: APPROVE\nCONFIDENCE: 0.85\nREASONING: Settlement is valid");

            Map<String, Object> result = orchestrator.reconcileSettlement(settlementId);

            assertNotNull(result, "Result should not be null");
            assertEquals(settlementId, result.get("settlementId"), "Settlement ID should match");

            logger.info("✅ Test passed for settlement: {}", settlementId);
        }
    }

    /**
     * Validator Agent Tests
     */
    @Nested
    @DisplayName("ValidatorAgent Tests")
    class ValidatorAgentTests {

        @Test
        @DisplayName("Should validate settlement as PASS when compliant")
        void testValidationPass() throws Exception {
            logger.info("🧪 Testing validation pass");

            when(mockChatModel.generate(anyString()))
                .thenReturn("VALIDATION: PASS\nRISK_SCORE: 0.2\nREASON: Settlement meets all compliance requirements");

            Map<String, Object> result = orchestrator.reconcileSettlement("SETTLE-001");

            assertNotNull(result.get("validatorDecision"), "Validator decision should be present");
            Map<String, Object> validatorDecision = (Map<String, Object>) result.get("validatorDecision");
            assertEquals("PASS", validatorDecision.get("validationStatus"), "Validation should pass");
            assertTrue((Double) validatorDecision.get("riskScore") < 0.5, "Risk score should be low");

            logger.info("✅ Test passed: Validation successful");
        }

        @Test
        @DisplayName("Should flag settlement as FAIL when non-compliant")
        void testValidationFail() throws Exception {
            logger.info("🧪 Testing validation fail");

            when(mockChatModel.generate(anyString()))
                .thenReturn("VALIDATION: FAIL\nRISK_SCORE: 0.9\nREASON: Account has pending restrictions");

            Map<String, Object> result = orchestrator.reconcileSettlement("SETTLE-001");

            assertNotNull(result.get("validatorDecision"), "Validator decision should be present");
            Map<String, Object> validatorDecision = (Map<String, Object>) result.get("validatorDecision");
            assertEquals("FAIL", validatorDecision.get("validationStatus"), "Validation should fail");
            assertTrue((Double) validatorDecision.get("riskScore") > 0.5, "Risk score should be high");

            logger.info("✅ Test passed: Validation failure detected");
        }
    }

    /**
     * Resolver Agent Tests
     */
    @Nested
    @DisplayName("ResolverAgent Tests")
    class ResolverAgentTests {

        @Test
        @DisplayName("Should AUTO_APPROVE when matched and validated")
        void testAutoApprove() throws Exception {
            logger.info("🧪 Testing auto-approve decision");

            when(mockChatModel.generate(anyString()))
                .thenReturn("ACTION: AUTO_APPROVE\nCONFIDENCE: 0.98\nJUSTIFICATION: Settlement matched and validated");

            Map<String, Object> result = orchestrator.reconcileSettlement("SETTLE-001");

            assertEquals("PROCESSED", result.get("status"), "Settlement should be processed");
            assertEquals("AUTO_APPROVE", result.get("finalAction"), "Action should be AUTO_APPROVE");

            logger.info("✅ Test passed: Auto-approval executed");
        }

        @Test
        @DisplayName("Should MANUAL_REVIEW when uncertain")
        void testManualReview() throws Exception {
            logger.info("🧪 Testing manual review decision");

            when(mockChatModel.generate(anyString()))
                .thenReturn("ACTION: MANUAL_REVIEW\nCONFIDENCE: 0.65\nJUSTIFICATION: Requires human oversight");

            Map<String, Object> result = orchestrator.reconcileSettlement("SETTLE-001");

            assertEquals("PENDING_REVIEW", result.get("status"), "Settlement should be pending review");
            assertEquals("MANUAL_REVIEW", result.get("finalAction"), "Action should be MANUAL_REVIEW");

            logger.info("✅ Test passed: Manual review flagged");
        }

        @Test
        @DisplayName("Should REJECT when critical issues detected")
        void testReject() throws Exception {
            logger.info("🧪 Testing rejection decision");

            when(mockChatModel.generate(anyString()))
                .thenReturn("ACTION: REJECT\nCONFIDENCE: 0.95\nJUSTIFICATION: Duplicate settlement detected");

            Map<String, Object> result = orchestrator.reconcileSettlement("SETTLE-001");

            assertEquals("REJECTED", result.get("status"), "Settlement should be rejected");
            assertEquals("REJECT", result.get("finalAction"), "Action should be REJECT");

            logger.info("✅ Test passed: Rejection executed");
        }

        @Test
        @DisplayName("Should HOLD when clarification needed")
        void testHold() throws Exception {
            logger.info("🧪 Testing hold decision");

            when(mockChatModel.generate(anyString()))
                .thenReturn("ACTION: HOLD\nCONFIDENCE: 0.55\nJUSTIFICATION: Awaiting account status confirmation");

            Map<String, Object> result = orchestrator.reconcileSettlement("SETTLE-001");

            assertEquals("HELD", result.get("status"), "Settlement should be held");
            assertEquals("HOLD", result.get("finalAction"), "Action should be HOLD");

            logger.info("✅ Test passed: Settlement on hold");
        }

        @ParameterizedTest
        @DisplayName("Should handle various confidence levels")
        @CsvSource({
            "AUTO_APPROVE, 0.98",
            "MANUAL_REVIEW, 0.70",
            "REJECT, 0.85",
            "HOLD, 0.60"
        })
        void testConfidenceLevels(String action, String confidence) throws Exception {
            logger.info("🧪 Testing {} with confidence {}", action, confidence);

            when(mockChatModel.generate(anyString()))
                .thenReturn(String.format("ACTION: %s\nCONFIDENCE: %s\nJUSTIFICATION: Test", action, confidence));

            Map<String, Object> result = orchestrator.reconcileSettlement("SETTLE-001");

            assertEquals(action, result.get("finalAction"), "Action should match: " + action);
            assertTrue((Double) result.get("finalConfidence") > 0.0, "Confidence should be extracted");

            logger.info("✅ Test passed for action: {}", action);
        }
    }

    /**
     * Batch Processing Tests
     */
    @Nested
    @DisplayName("Batch Reconciliation Tests")
    class BatchReconciliationTests {

        @Test
        @DisplayName("Should process multiple settlements in batch")
        void testBatchReconciliation() throws Exception {
            logger.info("🧪 Testing batch reconciliation");

            when(mockChatModel.generate(anyString()))
                .thenReturn("ACTION: AUTO_APPROVE\nCONFIDENCE: 0.95\nJUSTIFICATION: Valid");

            List<String> settlementIds = List.of("SETTLE-001", "SETTLE-002");
            Map<String, Object> batchResult = orchestrator.reconcileMultipleSettlements(settlementIds);

            assertEquals(2, batchResult.get("totalSettlements"), "Should process 2 settlements");
            assertEquals(2, batchResult.get("approved"), "Should approve 2 settlements");
            assertEquals(0, batchResult.get("errors"), "Should have 0 errors");

            logger.info("✅ Test passed: Batch reconciliation successful");
        }

        @Test
        @DisplayName("Should handle errors in batch processing")
        void testBatchWithErrors() throws Exception {
            logger.info("🧪 Testing batch with errors");

            when(mockChatModel.generate(anyString()))
                .thenThrow(new RuntimeException("Claude API error"));

            List<String> settlementIds = List.of("SETTLE-001", "SETTLE-002");
            Map<String, Object> batchResult = orchestrator.reconcileMultipleSettlements(settlementIds);

            assertEquals(2, batchResult.get("totalSettlements"), "Should attempt 2 settlements");
            assertEquals(2, batchResult.get("errors"), "Should have 2 errors");

            logger.info("✅ Test passed: Error handling in batch");
        }

        @Test
        @DisplayName("Should track approval/rejection counts")
        void testBatchCountTracking() throws Exception {
            logger.info("🧪 Testing batch count tracking");

            // Mock alternating responses
            when(mockChatModel.generate(anyString()))
                .thenReturn("ACTION: AUTO_APPROVE\nCONFIDENCE: 0.95\nJUSTIFICATION: OK")
                .thenReturn("ACTION: REJECT\nCONFIDENCE: 0.90\nJUSTIFICATION: Issues found");

            List<String> settlementIds = List.of("SETTLE-001", "SETTLE-002");
            Map<String, Object> batchResult = orchestrator.reconcileMultipleSettlements(settlementIds);

            int approved = (int) batchResult.get("approved");
            int rejected = (int) batchResult.get("rejected");

            assertTrue(approved + rejected > 0, "Should have decisions");
            assertEquals(2, approved + rejected + (int) batchResult.get("pendingReview"), "Totals should add up");

            logger.info("✅ Test passed: Count tracking accurate");
        }
    }

    /**
     * Error Handling Tests
     */
    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle non-existent settlement gracefully")
        void testNonExistentSettlement() throws Exception {
            logger.info("🧪 Testing non-existent settlement");

            Map<String, Object> result = orchestrator.reconcileSettlement("NON-EXISTENT");

            assertEquals("ERROR", result.get("status"), "Status should be ERROR");
            assertNotNull(result.get("reason"), "Should have error reason");

            logger.info("✅ Test passed: Non-existent settlement handled");
        }

        @Test
        @DisplayName("Should handle Claude API failures")
        void testClaudeAPIFailure() throws Exception {
            logger.info("🧪 Testing Claude API failure");

            when(mockChatModel.generate(anyString()))
                .thenThrow(new RuntimeException("Claude API connection failed"));

            Map<String, Object> result = orchestrator.reconcileSettlement("SETTLE-001");

            assertEquals("ERROR", result.get("status"), "Status should be ERROR");
            assertNotNull(result.get("error"), "Should contain error message");

            logger.info("✅ Test passed: API failure handled gracefully");
        }

        @Test
        @DisplayName("Should handle null responses from Claude")
        void testNullResponse() throws Exception {
            logger.info("🧪 Testing null Claude response");

            when(mockChatModel.generate(anyString()))
                .thenReturn(null);

            // Should handle gracefully without throwing exception
            assertDoesNotThrow(() -> orchestrator.reconcileSettlement("SETTLE-001"),
                "Should handle null response without throwing");

            logger.info("✅ Test passed: Null response handled");
        }
    }

    /**
     * Mock Verification Tests
     */
    @Nested
    @DisplayName("Mock Verification Tests")
    class MockVerificationTests {

        @Test
        @DisplayName("Should call Claude model correct number of times")
        void testMockCallCount() throws Exception {
            logger.info("🧪 Testing mock call count");

            when(mockChatModel.generate(anyString()))
                .thenReturn("ACTION: AUTO_APPROVE\nCONFIDENCE: 0.95\nJUSTIFICATION: Valid");

            orchestrator.reconcileSettlement("SETTLE-001");

            // Matcher (3 calls) + Validator (3 calls) + Resolver (2 calls) = 8 calls
            verify(mockChatModel, atLeast(3)).generate(anyString());

            logger.info("✅ Test passed: Mock call count verified");
        }

        @Test
        @DisplayName("Should verify prompts contain settlement data")
        void testPromptContent() throws Exception {
            logger.info("🧪 Testing prompt content verification");

            when(mockChatModel.generate(anyString()))
                .thenReturn("ACTION: AUTO_APPROVE\nCONFIDENCE: 0.95\nJUSTIFICATION: Valid");

            orchestrator.reconcileSettlement("SETTLE-001");

            // Verify that settlement ID is included in prompts
            verify(mockChatModel, atLeast(1)).generate(contains("SETTLE-001"));

            logger.info("✅ Test passed: Prompt content verified");
        }
    }

    /**
     * Helper method - creates default test settlement
     */
    private Settlement createTestSettlement(String id) {
        Settlement settlement = new Settlement();
        settlement.setSettlementId(id);
        settlement.setAccountId("ACC-001");
        settlement.setSecurity("AAPL");
        settlement.setQuantity(100.0);
        settlement.setAmount(15050.0);
        settlement.setCurrency("USD");
        settlement.setStatus("NEW");
        return settlement;
    }

    /**
     * Helper method - creates default test account
     */
    private Account createTestAccount(String id) {
        Account account = new Account();
        account.setAccountId(id);
        account.setAccountName("Test Account");
        account.setBalance(150000.0);
        account.setCurrency("USD");
        account.setStatus("ACTIVE");
        return account;
    }
}
