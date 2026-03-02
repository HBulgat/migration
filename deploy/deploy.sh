#!/bin/bash

# Exit immediately if a command exits with a non-zero status
set -e

# Change to the root directory of the project (assuming the script is run from the 'deploy' directory)
cd "$(dirname "$0")"

echo "=========================================="
echo "       Deploying Services (Docker)        "
echo "=========================================="

# Stop existing containers if running
echo "-> Stopping existing containers..."
docker-compose down

# Pull latest images
echo "-> Pulling latest multi-arch images..."
docker-compose pull

# Start new containers
echo "-> Starting containers..."
docker-compose up -d

echo "=========================================="
echo "          Deployment Complete!            "
echo "=========================================="
echo "Admin UI : http://localhost"
echo "Admin API: http://localhost:8080/doc.html"
echo "Diff API : http://localhost:8081/doc.html"
echo "=========================================="
