package com.agenticledger.agents.prompts;

import com.agenticledger.model.Settlement;
import com.agenticledger.model.Account;
import java.util.Map;

/**
 * PromptLibrary - Centralized repository for all agent prompts
 * Keeps prompts organized, versioned, and easy to maintain
 */
public class PromptLibrary {

    // ==================== MATCHER AGENT PROMPTS ====================

    /**
     * Matcher Agent - analyzes if settlement matches account position
     */
    public static String getMatcherPrompt(Settlement settlement, Account account) {
        return String.format(
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
    }

    /**
     * Devil's Advocate - challenges the matcher's analysis
     */
    public static String getDevilsAdvocatePrompt(Settlement settlement, Account account, String matcherAnalysis) {
        return String.format(
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
    }

    /**
     * Matcher Arbitrator - makes final decision
     */
    public static String getMatcherArbiterPrompt(Settlement settlement, Account account,
                                                   String matcherAnalysis, String devilsAdvocateAnalysis) {
        return String.format(
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
    }

    // ==================== VALIDATOR AGENT PROMPTS ====================

    /**
     * Validator - checks business rules and compliance
     */
    public static String getValidatorPrompt(Settlement settlement, Account account, Map<String, Object> matcherDecision) {
        return String.format(
            "You are a Validator Agent checking settlement compliance.\n\n" +
            "Settlement Details:\n" +
            "- ID: %s\n" +
            "- Status: %s\n" +
            "- Amount: $%f\n" +
            "- Security: %s\n" +
            "- Quantity: %f\n\n" +
            "Account Details:\n" +
            "- ID: %s\n" +
            "- Status: %s\n" +
            "- Balance: $%f\n\n" +
            "Matcher Decision: %s\n\n" +
            "Validation Checklist:\n" +
            "1. Is account status ACTIVE?\n" +
            "2. Is settlement amount within reasonable limits (>0, <$10M)?\n" +
            "3. Is security symbol valid (non-empty)?\n" +
            "4. Does settlement status indicate it hasn't been processed?\n" +
            "5. Are there any duplicate entries?\n\n" +
            "Provide validation report: PASS or FAIL with reasons.",

            settlement.getSettlementId(),
            settlement.getStatus(),
            settlement.getAmount(),
            settlement.getSecurity(),
            settlement.getQuantity(),
            account.getAccountId(),
            account.getStatus(),
            account.getBalance(),
            matcherDecision.get("finalDecision")
        );
    }

    /**
     * Skeptic - challenges the validation
     */
    public static String getSkepticPrompt(Settlement settlement, Account account, String validatorAnalysis) {
        return String.format(
            "You are a Skeptic Agent challenging the validation.\n\n" +
            "Settlement: %s, Amount: $%f\n" +
            "Account: %s, Status: %s\n\n" +
            "Validator's Analysis:\n%s\n\n" +
            "Challenge this validation with critical questions:\n" +
            "- What if this is a duplicate settlement processed today?\n" +
            "- What if the account has pending restrictions not yet visible?\n" +
            "- What if the amount violates a concentration limit?\n" +
            "- What if the security is under trading halt?\n" +
            "- What edge cases could slip through?\n\n" +
            "Provide risk assessment and counter-arguments.",

            settlement.getSettlementId(),
            settlement.getAmount(),
            account.getAccountId(),
            account.getStatus(),
            validatorAnalysis
        );
    }

    /**
     * Validator Arbitrator - makes validation decision
     */
    public static String getValidatorArbiterPrompt(Settlement settlement, Account account,
                                                     String validatorAnalysis, String skepticAnalysis,
                                                     Map<String, Object> matcherDecision) {
        return String.format(
            "You are an Arbitrator making the final validation decision.\n\n" +
            "Settlement: %s, Amount: $%f\n" +
            "Account: %s, Balance: $%f, Status: %s\n" +
            "Matcher Decision: %s\n\n" +
            "Validator says:\n%s\n\n" +
            "Skeptic says:\n%s\n\n" +
            "Make FINAL VALIDATION DECISION.\n" +
            "Respond in format:\n" +
            "VALIDATION: [PASS or FAIL]\n" +
            "RISK_SCORE: [0.0 to 1.0, where 1.0 is highest risk]\n" +
            "REASON: [one sentence]",

            settlement.getSettlementId(),
            settlement.getAmount(),
            account.getAccountId(),
            account.getBalance(),
            account.getStatus(),
            matcherDecision.get("finalDecision"),
            validatorAnalysis,
            skepticAnalysis
        );
    }

    // ==================== RESOLVER AGENT PROMPTS ====================

    /**
     * Resolver - proposes resolution strategies
     */
    public static String getResolverPrompt(Settlement settlement, Account account,
                                           Map<String, Object> matcherDecision,
                                           Map<String, Object> validatorDecision) {
        return String.format(
            "You are a Resolver Agent handling settlement conflicts.\n\n" +
            "Settlement: %s, Amount: $%f\n" +
            "Account: %s, Balance: $%f\n\n" +
            "Matcher Decision: %s\n" +
            "Validator Decision: %s\n" +
            "Risk Score: %f\n\n" +
            "Possible Resolution Actions:\n" +
            "1. AUTO_APPROVE - Settlement is good, process it\n" +
            "2. MANUAL_REVIEW - Needs human review\n" +
            "3. REJECT - Has critical issues, don't process\n" +
            "4. HOLD - Suspend until clarification\n" +
            "5. PARTIAL_APPROVE - Process partial amount\n\n" +
            "Recommend the best action with reasoning.",

            settlement.getSettlementId(),
            settlement.getAmount(),
            account.getAccountId(),
            account.getBalance(),
            matcherDecision.get("finalDecision"),
            validatorDecision.get("validationStatus"),
            validatorDecision.get("riskScore")
        );
    }

    /**
     * Conservative - challenges the resolver's decision
     */
    public static String getConservativePrompt(Settlement settlement, Account account, String resolverAnalysis) {
        return String.format(
            "You are a Conservative Agent reviewing resolution recommendations.\n\n" +
            "Settlement: %s, Amount: $%f\n" +
            "Account: %s, Status: %s\n\n" +
            "Resolver recommends:\n%s\n\n" +
            "Challenge this decision by asking:\n" +
            "- What's the downside if we're wrong?\n" +
            "- Have we validated all the prerequisites?\n" +
            "- What's the financial impact of approval vs. rejection?\n" +
            "- Are there regulatory implications?\n" +
            "- Should we be more cautious?\n\n" +
            "Provide risk mitigation recommendations.",

            settlement.getSettlementId(),
            settlement.getAmount(),
            account.getAccountId(),
            account.getStatus(),
            resolverAnalysis
        );
    }

    /**
     * Resolver Arbitrator - makes final resolution decision
     */
    public static String getResolverArbiterPrompt(Settlement settlement, Account account,
                                                   String resolverAnalysis, String conservativeAnalysis,
                                                   Map<String, Object> matcherDecision,
                                                   Map<String, Object> validatorDecision) {
        return String.format(
            "You are an Arbitrator making the FINAL RESOLUTION DECISION.\n\n" +
            "Settlement: %s, Amount: $%f\n" +
            "Account: %s, Balance: $%f\n" +
            "Matcher: %s | Validator: %s | Risk: %f\n\n" +
            "Resolver says:\n%s\n\n" +
            "Conservative says:\n%s\n\n" +
            "Make a BINDING DECISION on settlement action.\n" +
            "Response format:\n" +
            "ACTION: [AUTO_APPROVE | MANUAL_REVIEW | REJECT | HOLD | PARTIAL_APPROVE]\n" +
            "CONFIDENCE: [0.0 to 1.0]\n" +
            "JUSTIFICATION: [one sentence]",

            settlement.getSettlementId(),
            settlement.getAmount(),
            account.getAccountId(),
            account.getBalance(),
            matcherDecision.get("finalDecision"),
            validatorDecision.get("validationStatus"),
            validatorDecision.get("riskScore"),
            resolverAnalysis,
            conservativeAnalysis
        );
    }

    // ==================== SYSTEM PROMPTS ====================

    public static final String SYSTEM_PROMPT_BANKING =
        "You are an AI agent for a banking settlement reconciliation system. " +
        "Your role is to analyze settlements with rigorous accuracy. " +
        "Always consider compliance, risk, and financial impact. " +
        "Be conservative when uncertain.";

    public static final String SYSTEM_PROMPT_MATCHER =
        "You are a matching specialist. Analyze whether a settlement matches an account position. " +
        "Focus on factual accuracy of amounts, securities, and quantities.";

    public static final String SYSTEM_PROMPT_VALIDATOR =
        "You are a compliance validator. Check settlements against business rules, " +
        "regulations, and account restrictions. Identify risks and edge cases.";

    public static final String SYSTEM_PROMPT_RESOLVER =
        "You are a resolution specialist. When conflicts arise, propose pragmatic solutions " +
        "that balance approval speed with compliance requirements.";
}
