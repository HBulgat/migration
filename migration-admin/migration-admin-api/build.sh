#!/bin/bash
# Description: Build multi-arch (AMD64 & ARM64) Docker images locally
set -e

# Configuration (请根据需要修改这里的镜像名和版本号)
IMAGE_NAME="hbulgat/migration.admin.api"
VERSION="latest"

echo "=========================================="
echo "Building $IMAGE_NAME:$VERSION (amd64 & arm64)"
echo "=========================================="

# 如果还没有多架构 builder，则创建一个
if ! docker buildx ls | grep -q "multiarch-builder"; then
    echo "Creating new buildx builder 'multiarch-builder'..."
    docker buildx create --name multiarch-builder --use
    docker buildx inspect --bootstrap
else
    docker buildx use multiarch-builder
fi

# 1. 编译本机当前模块的 Jar (确保是最新的再打包)
echo "-> Compiling Java application..."
mvn clean package -DskipTests

# 2. 从本地打包出 amd64 镜像
echo "-> Building Docker image for linux/amd64..."
docker buildx build --platform linux/amd64 -t ${IMAGE_NAME}:${VERSION}-amd64 --load .

# 3. 从本地打包出 arm64 镜像
echo "-> Building Docker image for linux/arm64..."
docker buildx build --platform linux/arm64 -t ${IMAGE_NAME}:${VERSION}-arm64 --load .

echo ""
echo "✅ Build complete! Images are loaded into local Docker daemon."
docker images | grep "${IMAGE_NAME}"
echo ""
echo "Next step: Run ./upload.sh to push the images and create a manifest."
