package com.agenticledger.agents.mcp;

import com.agenticledger.model.Account;
import com.agenticledger.model.Settlement;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;


public class SettlementDatabaseMCP {

    private static final Logger logger = LoggerFactory.getLogger(SettlementDatabaseMCP.class);
    private final Firestore firestore;

    public SettlementDatabaseMCP(Firestore firestore) {
        this.firestore= firestore;
        logger.info("SettlementDatabaseMCP initialized");
    }

    // Method 1: Get settlement by ID
    public Settlement getSettlement(String settlementId) throws ExecutionException, InterruptedException {
        logger.info("Fetching settlement: {}", settlementId);

        try {
            DocumentSnapshot doc = firestore.collection("settlements")
                    .document(settlementId)
                    .get()
                    .get(); // Wait for the result

            if (!doc.exists()) {
                logger.warn("Settlement not found: {}", settlementId);
                return null;
            }

            // Convert Firestore document to Settlement object
            Settlement settlement = doc.toObject(Settlement.class);
            logger.info("Settlement fetched successfully: {}", settlementId);
            return settlement;

        } catch (Exception e) {
            logger.error("Error fetching settlement: {}", settlementId, e);
            throw new ExecutionException(e);
        }
    }

    // Method 2: Get account position by account ID
    public Account getAccountPosition(String accountId) throws ExecutionException, InterruptedException {
        logger.info("Fetching account position: {}", accountId);

        try {
            DocumentSnapshot doc = firestore.collection("accounts")
                    .document(accountId)
                    .get()
                    .get(); // Wait for the result

            if (!doc.exists()) {
                logger.warn("Account not found: {}", accountId);
                return null;
            }

            // Convert Firestore document to Account object
            Account account = doc.toObject(Account.class);
            logger.info("Account position fetched: {}", accountId);
            return account;

        } catch (Exception e) {
            logger.error("Error fetching account: {}", accountId, e);
            throw new ExecutionException(e);
        }
    }

    // Method 3: Update account position after settlement
    public void updatePosition(String accountId, Double quantity, String security) throws ExecutionException, InterruptedException {
        logger.info("Updating position for account: {} with {} shares of {}", accountId, quantity, security);

        try {
            // Get current account
            Account account = getAccountPosition(accountId);

            if (account == null) {
                logger.error("Cannot update: Account not found - {}", accountId);
                return;
            }

            // Update balance (simplified - in real system, would update positions array)
            Double newBalance = account.getBalance() + (quantity * 100); // Assuming price of 100
            account.setBalance(newBalance);

            // Write back to Firestore
            firestore.collection("accounts")
                    .document(accountId)
                    .set(account)
                    .get(); // Wait for completion

            logger.info("Position updated successfully for account: {}", accountId);

        } catch (Exception e) {
            logger.error("Error updating position: {}", accountId, e);
            throw new ExecutionException(e);
        }
    }

    // ToString for logging
    @Override
    public String toString() {
        return "SettlementDatabaseMCP{" +
                "firestore=" + firestore +
                '}';
    }
}


