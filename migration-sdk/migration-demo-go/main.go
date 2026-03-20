package main

import (
	"fmt"
	"log"

	"github.com/HBulgat/migration-sdk-go"
	"github.com/gin-gonic/gin"
)

func main() {
	// 1. 初始化 SDK 配置 (指向迁移平台的 Admin API 和 Diff 收集端点)
	config := &migration.Config{
		AdminUrl:       "http://localhost:8080", // migration-admin 的地址
		DiffServiceUrl: "http://localhost:8081", // migration-diff-service 的地址
	}

	// 2. 构造客户端
	client := migration.NewClient(config)

	// 3. 搭建 HTTP 服务
	r := gin.Default()

	// 4. 将业务路由和迁移组装分离到独立的 router.go 文件中注册
	RegisterRoutes(r, client)

	fmt.Println("🚀 Migration Demo Server starting on :8082")
	if err := r.Run(":8082"); err != nil {
		log.Fatal(err)
	}
}


