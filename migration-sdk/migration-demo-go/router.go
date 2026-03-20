package main

import (
	"errors"
	"log"
	"time"

	"github.com/HBulgat/migration-sdk-go"
	"github.com/HBulgat/migration-sdk-go/strategy"
	"github.com/gin-gonic/gin"
)

// ========= 模拟业务侧实体 =========

type User struct {
	ID    string `json:"id"`
	Name  string `json:"name"`
	Level int    `json:"level"`
}

// targetOld 模拟旧的服务调用 (可能是调数据库、旧微服务等)
func targetOld(args ...interface{}) (interface{}, error) {
	time.Sleep(50 * time.Millisecond)
	c := args[0].(*gin.Context)
	id := c.Param("id")

	if id == "err_old" {
		return nil, errors.New("old api internal server error")
	}

	return &User{ID: id, Name: "OldUser_" + id, Level: 1}, nil
}

// targetNew 模拟新的服务调用 (可能是调新微服务，或者访问了新的存储)
func targetNew(args ...interface{}) (interface{}, error) {
	time.Sleep(20 * time.Millisecond) // 新版做了性能优化
	c := args[0].(*gin.Context)
	id := c.Param("id")

	// 如果 ID 是 err_new，模拟报错
	if id == "err_new" {
		return nil, errors.New("new api unknown exception")
	}

	return &User{ID: id, Name: "NewUser_" + id, Level: 1}, nil
}

// targetFallback 模拟极端情况的兜底降级逻辑
func targetFallback(err error, args ...interface{}) (interface{}, error) {
	log.Printf("[Fallback] triggered due to error: %v\n", err)
	c := args[0].(*gin.Context)
	id := c.Param("id")
	// 返回一个安全的默认值
	return &User{ID: id, Name: "FallbackUser_" + id, Level: 0}, errors.New("fallback executed")
}

// userParamHandler 针对此接口抽取灰度参数的规则
func userParamHandler(args ...interface{}) map[string]interface{} {
	c := args[0].(*gin.Context)
	return map[string]interface{}{
		"userId": c.Param("id"),
		"level":  5, // 模拟参数 level
	}
}

// migrationWrap 统一的简写泛型封装：让 target 函数完全接管 gin.Context
func migrationWrap[T any](
	client *migration.Client,
	key string,
	oldFn strategy.TargetFunc,
	newFn strategy.TargetFunc,
	fallbackFn strategy.FallbackFunc,
	mapBuilder strategy.ParamHandler,
) gin.HandlerFunc {
	// 1. 初始化灰度策略与执行函数
	executeFn := client.Wrap(key, oldFn, newFn, fallbackFn, mapBuilder)
	// 2. 返回被通用泛型函数包裹好的 Gin HTTP Handler
	return HandleWithMigration[T](executeFn, func(c *gin.Context) []interface{} {
		return []interface{}{c} // 核心魔法：以后 SDK args[0] 全是 *gin.Context ！
	})
}

// RegisterRoutes 统一注册所有的路由和迁移策略包装，不污染 main 函数
func RegisterRoutes(r *gin.Engine, client *migration.Client) {

	// 极简路由绑定：完全按照您的期望，一句搞定！
	r.GET("/api/v1/user/:id", migrationWrap[User](client, "user-getUser-api", targetOld, targetNew, targetFallback, userParamHandler))
}

