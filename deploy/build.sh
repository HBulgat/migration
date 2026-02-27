#!/bin/bash

# 检查.env文件是否存在
if [ ! -f .env ]; then
  echo "Error: .env file not found!"
  echo "Please copy .env.example to .env and modify the configuration."
  exit 1
fi

# 设置部署模式
if [ $# -eq 1 ]; then
  export DEPLOY_MODE=$1
else
  export DEPLOY_MODE=local
fi

# 加载环境变量
source .env

# 构建管理后台API
echo "Building migration-admin-api..."
cd ../migration-admin/migration-admin-api
mvn clean package -DskipTests

# 构建Diff服务
echo "Building migration-diff..."
cd ../../migration-diff
mvn clean package -DskipTests

# 构建前端项目
echo "Building migration-admin-ui..."
cd ../migration-admin/migration-admin-ui
npm install
npm run build

# 构建Docker镜像
echo "Building Docker images..."
cd ../../../deploy
docker-compose build

echo "Build completed successfully!"