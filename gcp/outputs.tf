output "settlement_topic" {
  value = google_pubsub_topic.settlement_events.name
}

output "corporate_actions_topic" {
  value = google_pubsub_topic.corporate_actions.name
}

output "firestore_database" {
  value = google_firestore_database.database.name
}

output "bigquery_dataset" {
  value = google_bigquery_dataset.audit_logs.dataset_id
}