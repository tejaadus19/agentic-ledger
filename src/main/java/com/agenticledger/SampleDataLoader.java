package com.agenticledger;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.agenticledger.model.Settlement;
import com.agenticledger.model.Account;
import java.util.HashMap;
import java.util.Map;

public class SampleDataLoader {

    public static void main(String[] args) throws Exception {
        // Connect to real GCP Firestore (not emulator)
        FirestoreOptions options = FirestoreOptions.newBuilder()
                .setProjectId("trust-agent-service")
                .build();

        Firestore db = options.getService();

        System.out.println("Connected to Firestore emulator!");

        // Create sample accounts
        createSampleAccounts(db);

        // Create sample settlements
        createSampleSettlements(db);

        System.out.println("✅ Sample data loaded successfully!");

        db.close();
    }

    private static void createSampleAccounts(Firestore db) throws Exception {
        System.out.println("\n📝 Creating sample accounts...");

        // Account 1
        Map<String, Object> account1 = new HashMap<>();
        account1.put("accountId", "ACC-001");
        account1.put("accountName", "Trust Account A");
        account1.put("balance", 150000.0);
        account1.put("currency", "USD");
        account1.put("status", "ACTIVE");

        db.collection("accounts").document("ACC-001").set(account1).get();
        System.out.println("✅ Created ACC-001");

        // Account 2
        Map<String, Object> account2 = new HashMap<>();
        account2.put("accountId", "ACC-002");
        account2.put("accountName", "Trust Account B");
        account2.put("balance", 250000.0);
        account2.put("currency", "USD");
        account2.put("status", "ACTIVE");

        db.collection("accounts").document("ACC-002").set(account2).get();
        System.out.println("✅ Created ACC-002");
    }

    private static void createSampleSettlements(Firestore db) throws Exception {
        System.out.println("\n📝 Creating sample settlements...");

        // Settlement 1
        Map<String, Object> settlement1 = new HashMap<>();
        settlement1.put("settlementId", "SETTLE-001");
        settlement1.put("accountId", "ACC-001");
        settlement1.put("security", "AAPL");
        settlement1.put("quantity", 100.0);
        settlement1.put("amount", 15050.0);
        settlement1.put("currency", "USD");
        settlement1.put("status", "NEW");

        db.collection("settlements").document("SETTLE-001").set(settlement1).get();
        System.out.println("✅ Created SETTLE-001");

        // Settlement 2
        Map<String, Object> settlement2 = new HashMap<>();
        settlement2.put("settlementId", "SETTLE-002");
        settlement2.put("accountId", "ACC-002");
        settlement2.put("security", "MSFT");
        settlement2.put("quantity", 50.0);
        settlement2.put("amount", 17500.0);
        settlement2.put("currency", "USD");
        settlement2.put("status", "NEW");

        db.collection("settlements").document("SETTLE-002").set(settlement2).get();
        System.out.println("✅ Created SETTLE-002");
    }
}
