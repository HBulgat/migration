#!/bin/bash
# Description: Push AMD64 & ARM64 images to registry and stitch them into a multi-arch manifest
set -e

# Configuration (请确保与 build.sh 中的保持一致)
IMAGE_NAME="hbulgat/migration.admin.api"
VERSION="latest"

echo "=========================================="
echo "Uploading multi-arch image: $IMAGE_NAME:$VERSION"
echo "=========================================="

# 1. Push 两个独立架构的镜像到远端仓库
echo "-> Pushing amd64 image..."
docker push ${IMAGE_NAME}:${VERSION}-amd64

echo "-> Pushing arm64 image..."
docker push ${IMAGE_NAME}:${VERSION}-arm64

# 2. 在本地组合 Manifest 清单
echo "-> Creating multi-arch manifest..."
# 如果旧缓存存在，先删除以防报错
docker manifest rm ${IMAGE_NAME}:${VERSION} 2>/dev/null || true
docker manifest create ${IMAGE_NAME}:${VERSION} \
  ${IMAGE_NAME}:${VERSION}-amd64 \
  ${IMAGE_NAME}:${VERSION}-arm64

# 3. 标注架构信息
echo "-> Annotating manifest..."
docker manifest annotate ${IMAGE_NAME}:${VERSION} ${IMAGE_NAME}:${VERSION}-amd64 --os linux --arch amd64
docker manifest annotate ${IMAGE_NAME}:${VERSION} ${IMAGE_NAME}:${VERSION}-arm64 --os linux --arch arm64

# 4. 将合并后的清单推送到仓库
echo "-> Pushing multi-arch manifest..."
docker manifest push ${IMAGE_NAME}:${VERSION}

echo ""
echo "✅ Upload complete! $IMAGE_NAME:$VERSION is now a multi-arch verified image on the registry."
