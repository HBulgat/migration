#!/bin/bash

# 检查.env文件是否存在
if [ ! -f .env ]; then
  echo "Error: .env file not found!"
  echo "Please copy .env.example to .env and modify the configuration."
  exit 1
fi

# 加载环境变量
export $(cat .env | grep -v '^#' | xargs)

# 停止所有服务
echo "Stopping all services..."
docker-compose down

echo "Services stopped successfully!"