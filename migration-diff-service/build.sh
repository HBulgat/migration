#!/bin/bash
# Description: Build multi-arch (AMD64 & ARM64) Docker images locally
set -e

# Configuration
IMAGE_NAME="hbulgat/migration.diff.service"
VERSION="latest"

echo "=========================================="
echo "Building $IMAGE_NAME:$VERSION (amd64 & arm64)"
echo "=========================================="

if ! docker buildx ls | grep -q "multiarch-builder"; then
    echo "Creating new buildx builder 'multiarch-builder'..."
    docker buildx create --name multiarch-builder --use
    docker buildx inspect --bootstrap
else
    docker buildx use multiarch-builder
fi

# 1. 编译本机当前模块的 Jar (从根目录构建以确保依赖正确解析)
echo "-> Compiling Java application..."
(cd .. && mvn clean package -DskipTests -pl migration-diff-service -am)

echo "-> Building Docker image for linux/amd64..."
docker buildx build --platform linux/amd64 -t ${IMAGE_NAME}:${VERSION}-amd64 --load .

echo "-> Building Docker image for linux/arm64..."
docker buildx build --platform linux/arm64 -t ${IMAGE_NAME}:${VERSION}-arm64 --load .

echo ""
echo "✅ Build complete! Images are loaded into local Docker daemon."
docker images | grep "${IMAGE_NAME}"
echo ""
echo "Next step: Run ./upload.sh to push the images and create a manifest."
