#!/bin/bash

# 检查.env文件是否存在
if [ ! -f .env ]; then
  echo "Error: .env file not found!"
  echo "Please copy .env.example to .env and modify the configuration."
  exit 1
fi

# 加载环境变量
export $(cat .env | grep -v '^#' | xargs)

# 启动所有服务
echo "Starting all services..."
docker-compose up -d

echo "Services started successfully!"
echo "Admin UI: http://localhost"
echo "Admin API: http://localhost:8080"
echo "Diff Service: http://localhost:8081"