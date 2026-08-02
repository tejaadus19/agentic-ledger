# Pub/Sub Topic for Settlement Events
resource "google_pubsub_topic" "settlement_events" {
  name = "settlement-events"
  labels = {
    environment = var.environment
    service     = "trust-agent"
    use_case    = "reconciliation"
  }
  message_retention_duration = "86400s"
}

# Pub/Sub Topic for Corporate Actions
resource "google_pubsub_topic" "corporate_actions" {
  name = "corporate-actions"
  labels = {
    environment = var.environment
    service     = "trust-agent"
    use_case    = "corporate-actions"
  }
  message_retention_duration = "86400s"
}

# Pub/Sub Subscription for Settlement Events
resource "google_pubsub_subscription" "settlement_events_sub" {
  name             = "settlement-events-sub"
  topic            = google_pubsub_topic.settlement_events.name
  ack_deadline_seconds = 20
}

# Pub/Sub Subscription for Corporate Actions
resource "google_pubsub_subscription" "corporate_actions_sub" {
  name             = "corporate-actions-sub"
  topic            = google_pubsub_topic.corporate_actions.name
  ack_deadline_seconds = 20
}

# Firestore Database
resource "google_firestore_database" "database" {
  project     = var.project_id
  name        = "projects/${var.project_id}/databases/(default)"
  location_id = var.region
  type        = "FIRESTORE_NATIVE"
}

# BigQuery Dataset for Audit Logs
resource "google_bigquery_dataset" "audit_logs" {
  dataset_id = "trust_agent_audit_logs"
  location   = var.region

  labels = {
    environment = var.environment
    service     = "trust-agent"
  }
}

# BigQuery Table: Reconciliation Decisions
resource "google_bigquery_table" "reconciliation_decisions" {
  dataset_id = google_bigquery_dataset.audit_logs.dataset_id
  table_id   = "reconciliation_decisions"

  schema = jsonencode([
    {
      name        = "timestamp"
      type        = "TIMESTAMP"
      mode        = "REQUIRED"
      description = "When the reconciliation decision was made"
    },
    {
      name        = "settlementId"
      type        = "STRING"
      mode        = "REQUIRED"
      description = "Settlement message ID"
    },
    {
      name        = "accountId"
      type        = "STRING"
      mode        = "REQUIRED"
      description = "Trust account ID"
    },
    {
      name        = "security"
      type        = "STRING"
      mode        = "NULLABLE"
      description = "Security/instrument (e.g., AAPL, MSFT)"
    },
    {
      name        = "quantity"
      type        = "FLOAT64"
      mode        = "NULLABLE"
      description = "Quantity in settlement"
    },
    {
      name        = "matcherDecision"
      type        = "STRING"
      mode        = "NULLABLE"
      description = "Matcher agent decision (MATCH/MISMATCH)"
    },
    {
      name        = "validatorDecision"
      type        = "STRING"
      mode        = "NULLABLE"
      description = "Validator agent decision (VALID/INVALID)"
    },
    {
      name        = "finalDecision"
      type        = "STRING"
      mode        = "REQUIRED"
      description = "Final decision (APPROVE/REJECT/ESCALATE)"
    },
    {
      name        = "confidence"
      type        = "FLOAT64"
      mode        = "NULLABLE"
      description = "Confidence score (0.0-1.0)"
    },
    {
      name        = "reasoning"
      type        = "STRING"
      mode        = "NULLABLE"
      description = "Agent reasoning/explanation"
    }
  ])
}

# BigQuery Table: Corporate Action Events
resource "google_bigquery_table" "corporate_action_events" {
  dataset_id = google_bigquery_dataset.audit_logs.dataset_id
  table_id   = "corporate_action_events"

  schema = jsonencode([
    {
      name        = "timestamp"
      type        = "TIMESTAMP"
      mode        = "REQUIRED"
      description = "When the corporate action was processed"
    },
    {
      name        = "actionId"
      type        = "STRING"
      mode        = "REQUIRED"
      description = "Corporate action event ID"
    },
    {
      name        = "actionType"
      type        = "STRING"
      mode        = "REQUIRED"
      description = "Type of action (DIVIDEND, STOCK_SPLIT, MERGER, BONUS)"
    },
    {
      name        = "security"
      type        = "STRING"
      mode        = "REQUIRED"
      description = "Security affected (e.g., AAPL)"
    },
    {
      name        = "detectorDecision"
      type        = "STRING"
      mode        = "NULLABLE"
      description = "Detector agent decision"
    },
    {
      name        = "processorDecision"
      type        = "STRING"
      mode        = "NULLABLE"
      description = "Processor agent decision"
    },
    {
      name        = "executorDecision"
      type        = "STRING"
      mode        = "NULLABLE"
      description = "Executor agent decision"
    },
    {
      name        = "finalDecision"
      type        = "STRING"
      mode        = "REQUIRED"
      description = "Final decision (EXECUTE/ESCALATE/REJECT)"
    },
    {
      name        = "affectedAccounts"
      type        = "INTEGER"
      mode        = "NULLABLE"
      description = "Number of accounts affected"
    },
    {
      name        = "reasoning"
      type        = "STRING"
      mode        = "NULLABLE"
      description = "Agent reasoning/explanation"
    }
  ])
}

# BigQuery Table: Agent Execution Logs
resource "google_bigquery_table" "agent_execution_logs" {
  dataset_id = google_bigquery_dataset.audit_logs.dataset_id
  table_id   = "agent_execution_logs"

  schema = jsonencode([
    {
      name        = "timestamp"
      type        = "TIMESTAMP"
      mode        = "REQUIRED"
      description = "When agent executed"
    },
    {
      name        = "agentName"
      type        = "STRING"
      mode        = "REQUIRED"
      description = "Name of agent (MatcherAgent, ValidatorAgent, etc)"
    },
    {
      name        = "eventId"
      type        = "STRING"
      mode        = "REQUIRED"
      description = "Settlement or action ID being processed"
    },
    {
      name        = "mcpToolsUsed"
      type        = "STRING"
      mode        = "NULLABLE"
      description = "MCP tools called by agent"
    },
    {
      name        = "executionTimeMs"
      type        = "INTEGER"
      mode        = "NULLABLE"
      description = "Execution time in milliseconds"
    },
    {
      name        = "decision"
      type        = "STRING"
      mode        = "NULLABLE"
      description = "Agent's decision"
    },
    {
      name        = "confidenceScore"
      type        = "FLOAT64"
      mode        = "NULLABLE"
      description = "Confidence in decision (0.0-1.0)"
    },
    {
      name        = "status"
      type        = "STRING"
      mode        = "REQUIRED"
      description = "SUCCESS, FAILED, ERROR"
    }
  ])
}
