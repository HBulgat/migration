package main

import (
	"fmt"
	"log"

	"github.com/HBulgat/migration-demo-go/config"
	"github.com/HBulgat/migration-sdk-go"
	"github.com/gin-gonic/gin"
)

var client *migration.Client

func main() {

	// 2. 初始化迁移 SDK
	client = migration.NewClient(&config.Config.Migration)

	// 3. 搭建 HTTP 服务
	r := gin.Default()

	// 4. 将业务路由和迁移组装分离到独立的 router.go 文件中注册
	RegisterRoutes(r)

	port := 8082
	if config.Config.Server.Port != 0 {
		port = config.Config.Server.Port
	}
	addr := fmt.Sprintf(":%d", port)

	fmt.Printf("🚀 Migration Demo Server starting on %s\n", addr)
	if err := r.Run(addr); err != nil {
		log.Fatal(err)
	}
}
