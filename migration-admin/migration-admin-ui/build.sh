#!/bin/bash
# Description: Build multi-arch (AMD64 & ARM64) Docker images locally
set -e

# Configuration
IMAGE_NAME="hbulgat/migration.admin.ui"
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

# 前台 UI 构建依赖 Node 环境，Dockerfile 中已经是基于多阶段构建的了，所以我们可以直接执行 build
echo "-> Building Docker image for linux/amd64..."
docker buildx build --platform linux/amd64 -t ${IMAGE_NAME}:${VERSION}-amd64 --load .

echo "-> Building Docker image for linux/arm64..."
docker buildx build --platform linux/arm64 -t ${IMAGE_NAME}:${VERSION}-arm64 --load .

echo ""
echo "✅ Build complete! Images are loaded into local Docker daemon."
docker images | grep "${IMAGE_NAME}"
echo ""
echo "Next step: Run ./upload.sh to push the images and create a manifest."
