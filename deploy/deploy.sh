#!/bin/bash

# Exit immediately if a command exits with a non-zero status
set -e

# Change to the root directory of the project (assuming the script is run from the 'deploy' directory)
cd "$(dirname "$0")/.."

echo "=========================================="
echo "    Building Backend Services (Maven)     "
echo "=========================================="
mvn clean package -DskipTests
echo "✅ Backend build SUCCESS"
echo ""

echo "=========================================="
echo "       Deploying Services (Docker)        "
echo "=========================================="
# Change back to deploy directory
cd deploy

# Stop existing containers if running
echo "Stopping existing containers..."
docker-compose down

# Build and start new containers
echo "Building Docker images and starting containers..."
docker-compose up -d --build

echo "=========================================="
echo "          Deployment Complete!            "
echo "=========================================="
echo "Admin UI : http://localhost"
echo "Admin API: http://localhost:8080/doc.html"
echo "Diff API : http://localhost:8081/doc.html"
echo "=========================================="
