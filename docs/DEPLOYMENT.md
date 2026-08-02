# Deployment Guide

## Local Development Setup

### Prerequisites
- Java 21
- Maven 3.9+
- Google Cloud SDK
- Docker (for emulators)

### 1. Install Google Cloud SDK

```bash
brew install --cask google-cloud-sdk
gcloud init
```

### 2. Set Up Local Emulators

Create `scripts/start-emulators.sh`:

```bash
#!/bin/bash

echo "Starting Firestore Emulator..."
gcloud beta emulators firestore start --host-port=localhost:8080 &
FIRESTORE_PID=$!

echo "Starting Pub/Sub Emulator..."
gcloud beta emulators pubsub start --host-port=localhost:8085 &
PUBSUB_PID=$!

echo "Emulators started!"
echo "Firestore: http://localhost:8080"
echo "Pub/Sub: http://localhost:8085"
echo ""
echo "To stop emulators, run: kill $FIRESTORE_PID $PUBSUB_PID"

wait
```

Make it executable:
```bash
chmod +x scripts/start-emulators.sh
./scripts/start-emulators.sh
```

### 3. Run Tests

```bash
mvn clean test
```

### 4. Run Application

```bash
mvn exec:java -Dexec.mainClass="com.deutschebank.core.Application"
```

## GCP Deployment

### Prerequisites
- GCP account (free tier)
- Cloud SDK configured
- Docker installed

### 1. Enable APIs

```bash
gcloud services enable run.googleapis.com pubsub.googleapis.com firestore.googleapis.com
```

### 2. Build Docker Image

```bash
docker build -t gcr.io/PROJECT_ID/trust-agent-service:latest .
docker push gcr.io/PROJECT_ID/trust-agent-service:latest
```

Replace `PROJECT_ID` with your GCP project ID.

### 3. Deploy to Cloud Run

```bash
gcloud run deploy trust-agent-service \
  --image gcr.io/PROJECT_ID/trust-agent-service:latest \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --set-env-vars="FIRESTORE_PROJECT_ID=PROJECT_ID,PUBSUB_PROJECT_ID=PROJECT_ID"
```

### 4. Verify Deployment

```bash
gcloud run services describe trust-agent-service --region us-central1
```

## CI/CD with GitHub Actions

Create `.github/workflows/tests.yml`:

```yaml
name: Tests

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up Java
        uses: actions/setup-java@v2
        with:
          java-version: '21'
          distribution: 'adopt'
      - name: Run tests
        run: mvn clean test
```

## Troubleshooting

### Emulator Connection Issues
```bash
export FIRESTORE_EMULATOR_HOST=localhost:8080
export PUBSUB_EMULATOR_HOST=localhost:8085
mvn test
```

### Clear Emulator State
```bash
rm -rf ~/.config/gcloud/emulators
```
