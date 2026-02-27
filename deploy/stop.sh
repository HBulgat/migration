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

# 停止所有服务
echo "Stopping all services..."
docker-compose down

echo "Services stopped successfully!"