#!/bin/bash
# Description: Push AMD64 & ARM64 images to registry and stitch them into a multi-arch manifest
set -e

# Configuration 
IMAGE_NAME="hbulgat/migration.diff.service"
VERSION="latest"

echo "=========================================="
echo "Uploading multi-arch image: $IMAGE_NAME:$VERSION"
echo "=========================================="

echo "-> Pushing amd64 image..."
docker push ${IMAGE_NAME}:${VERSION}-amd64

echo "-> Pushing arm64 image..."
docker push ${IMAGE_NAME}:${VERSION}-arm64

echo "-> Creating multi-arch manifest..."
docker manifest rm ${IMAGE_NAME}:${VERSION} 2>/dev/null || true
docker manifest create ${IMAGE_NAME}:${VERSION} \
  ${IMAGE_NAME}:${VERSION}-amd64 \
  ${IMAGE_NAME}:${VERSION}-arm64

echo "-> Annotating manifest..."
docker manifest annotate ${IMAGE_NAME}:${VERSION} ${IMAGE_NAME}:${VERSION}-amd64 --os linux --arch amd64
docker manifest annotate ${IMAGE_NAME}:${VERSION} ${IMAGE_NAME}:${VERSION}-arm64 --os linux --arch arm64

echo "-> Pushing multi-arch manifest..."
docker manifest push ${IMAGE_NAME}:${VERSION}

echo ""
echo "✅ Upload complete! $IMAGE_NAME:$VERSION is now a multi-arch verified image on the registry."
