package com.agenticledger.agents.mcp;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.agenticledger.model.Settlement;
import com.agenticledger.model.Account;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

public class SettlementDatabaseMCPTest {
    private static final Logger logger = LoggerFactory.getLogger(SettlementDatabaseMCPTest.class);
    private static Firestore firestore;
    private SettlementDatabaseMCP mcp;

    @BeforeAll
    public static void setUpFirestore() {
        // Connect to Firestore emulator
        System.setProperty("FIRESTORE_EMULATOR_HOST", "localhost:8080");
        firestore = FirestoreOptions.getDefaultInstance().getService();
        logger.info("✅ Connected to Firestore emulator");
    }

    @BeforeEach
    public void setUp() {
        // Initialize MCP with Firestore connection
        mcp = new SettlementDatabaseMCP(firestore);
        logger.info("✅ SettlementDatabaseMCP initialized");
    }

    /**
     * Test: Get settlement by ID
     * Expected: Settlement data should match sample data
     */
    @Test
    public void testGetSettlement() throws Exception {
        logger.info("🧪 Running testGetSettlement...");

        // Act
        Settlement settlement = mcp.getSettlement("SETTLE-001");

        // Assert
        assertNotNull(settlement, "Settlement should not be null");
        assertEquals("SETTLE-001", settlement.getSettlementId(), "Settlement ID should match");
        assertEquals("ACC-001", settlement.getAccountId(), "Account ID should match");
        assertEquals("AAPL", settlement.getSecurity(), "Security should be AAPL");
        assertEquals(100.0, settlement.getQuantity(), "Quantity should be 100");
        assertEquals(15050.0, settlement.getAmount(), "Amount should be 15050");

        logger.info("✅ testGetSettlement passed!");
    }

    /**
     * Test: Get account position by ID
     * Expected: Account data should match sample data
     */
    @Test
    public void testGetAccountPosition() throws Exception {
        logger.info("🧪 Running testGetAccountPosition...");

        // Act
        Account account = mcp.getAccountPosition("ACC-001");

        // Assert
        assertNotNull(account, "Account should not be null");
        assertEquals("ACC-001", account.getAccountId(), "Account ID should match");
        assertEquals("Trust Account A", account.getAccountName(), "Account name should match");
        assertEquals(150000.0, account.getBalance(), "Balance should be 150000");
        assertEquals("USD", account.getCurrency(), "Currency should be USD");
        assertEquals("ACTIVE", account.getStatus(), "Status should be ACTIVE");

        logger.info("✅ testGetAccountPosition passed!");
    }

    /**
     * Test: Update account position
     * Expected: Account balance should be updated
     */
    @Test
    public void testUpdatePosition() throws Exception {
        logger.info("🧪 Running testUpdatePosition...");

        // Arrange
        Double originalBalance = 150000.0;
        Double quantity = 50.0; // 50 shares at $100 = $5000

        // Act
        mcp.updatePosition("ACC-001", quantity, "AAPL");

        // Assert - Get updated account
        Account updatedAccount = mcp.getAccountPosition("ACC-001");
        assertNotNull(updatedAccount, "Updated account should not be null");

        Double expectedBalance = originalBalance + (quantity * 100);
        assertEquals(expectedBalance, updatedAccount.getBalance(), "Balance should be updated");

        logger.info("✅ testUpdatePosition passed!");
    }

    /**
     * Test: Get non-existent settlement
     * Expected: Should return null
     */
    @Test
    public void testGetNonExistentSettlement() throws Exception {
        logger.info("🧪 Running testGetNonExistentSettlement...");

        // Act
        Settlement settlement = mcp.getSettlement("NON-EXISTENT");

        // Assert
        assertNull(settlement, "Non-existent settlement should return null");

        logger.info("✅ testGetNonExistentSettlement passed!");
    }

    /**
     * Test: MCP initialization and logging
     * Expected: MCP should initialize with proper logging
     */
    @Test
    public void testMCPInitialization() {
        logger.info("🧪 Running testMCPInitialization...");

        // Assert
        assertNotNull(mcp, "MCP should be initialized");
        assertNotNull(mcp.toString(), "MCP toString should work");
        assertTrue(mcp.toString().contains("SettlementDatabaseMCP"), "toString should contain class name");

        logger.info("✅ testMCPInitialization passed!");
    }
}
