# Trust Agent Service - Architecture

## Overview

Production-grade agentic infrastructure for banking operations using Claude AI.

**Two Core Use Cases:**
1. **Real-time Transaction Reconciliation** - Adversarial agent pattern
2. **Automated Corporate Actions Processing** - Circular refinement pattern

## System Architecture

```
Settlement/Corp Action Events
    ↓
Pub/Sub (Event Stream)
    ↓
Agent Orchestrator (Cloud Run)
    ↓
Agent Pipeline (Reconciliation OR Corporate Actions)
    ↓
MCP Servers (Database abstractions)
    ↓
State Store (Firestore - Strong Consistency)
    ↓
Audit Trail (BigQuery)
```

## Agent Patterns

### Reconciliation: Adversarial Pattern
- **Agent 1 (Match Maker)**: Compares settlement with account position
- **Agent 2 (Devil's Advocate)**: Finds edge cases & potential issues
- **Arbitrator**: Synthesizes both perspectives → Final decision

### Corporate Actions: Circular Refinement
- **Round 1**: Policy Agent analyzes rules
- **Round 2**: Compliance Agent validates regulations
- **Round 3**: Risk Agent evaluates impact
- **Resolution**: Converges to consensus if conflicts exist

## Technology Stack

| Component | Technology | Why |
|-----------|-----------|-----|
| Language | Java 21 | Strong typing, performance |
| Agents | LangChain4j + Claude API | Agentic reasoning |
| Tool Integration | MCP (Model Context Protocol) | Security + auditability |
| Event Streaming | GCP Pub/Sub | <50ms latency |
| State Store | GCP Firestore | ACID for financial data |
| Serverless | GCP Cloud Run | Auto-scaling, pay-per-request |
| Analytics | BigQuery | Audit trail + analytics |

## Data Storage Strategy (Free Tier)

### Firestore Collections (1GB free + 50k reads/day)

```
trust-agent-service/
├── accounts/
│   └── {accountId}/
│       ├── positions (array of holdings)
│       ├── reconciliations (sub-collection)
│       └── corporateActions (sub-collection)
├── settlements/
│   └── {settlementId}/
│       ├── status (NEW, MATCHED, RECONCILED, FAILED)
│       ├── timestamp
│       └── agentDecisions (sub-collection)
├── corporateActions/
│   └── {actionId}/
│       ├── type (DIVIDEND, SPLIT, MERGE)
│       ├── status
│       └── impacts (sub-collection)
└── mcp-tools/
    └── configuration (MCP tool definitions)
```

### BigQuery (1TB free query/month)

```
audit_logs.reconciliation_decisions
  ├── timestamp
  ├── settlementId
  ├── accountId
  ├── agentDecisions (JSON)
  ├── finalDecision
  └── confidence

audit_logs.corporate_action_events
  ├── timestamp
  ├── actionId
  ├── type
  ├── agentDecisions (JSON)
  └── executionResult
```

## Performance Targets

- **Reconciliation latency**: <500ms per transaction
- **Corporate Actions latency**: <2 seconds per event
- **Throughput**: 100k+ reconciliations/hour
- **State consistency**: ACID transactions (Firestore)
- **Audit trail**: 100% of agent decisions logged

## Deployment

### Local Development
- Firestore Emulator (port 8080)
- Pub/Sub Emulator (port 8085)
- Java agent execution
- Full integration testing

### GCP Production
- Cloud Run (serverless Java container)
- Cloud Pub/Sub (managed event streaming)
- Cloud Firestore (managed NoSQL)
- BigQuery (managed data warehouse)
- Cloud Logging (structured logs)

## Cost Model (Free Tier)

| Service | Free Tier | Your Usage | Cost |
|---------|-----------|-----------|------|
| Cloud Run | 2M requests/month | ~1k/month | $0 |
| Pub/Sub | 10GB messages/month | ~100MB/month | $0 |
| Firestore | 1GB + 50k reads/day | ~10GB + 100 reads/day | $0 |
| BigQuery | 1TB query/month | ~10GB/month | $0 |
| Cloud Logging | 50GB/month | ~5GB/month | $0 |
| **Total** | | | **$0** |

Production cost (at scale): ~$50-200/month

## Next Steps

1. Local development with emulators
2. Agent implementation (reconciliation pipeline first)
3. MCP server implementation
4. Integration testing
5. Cloud deployment
