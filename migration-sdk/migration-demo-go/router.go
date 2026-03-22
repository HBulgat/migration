package main

import (
	"github.com/HBulgat/migration-demo-go/handler"
	"github.com/HBulgat/migration-sdk-go"
	"github.com/gin-gonic/gin"
)

// RegisterRoutes 统一注册所有的路由和迁移策略包装，无需污染业务代码
func RegisterRoutes(r *gin.Engine) {
	// 极简泛型路由绑定
	r.GET("/api/v1/user/:id", migrationWrap(
		"get-user-by-id",
		handler.UserParamHandler,
		handler.OldGetUserById,
		handler.NewGetUserById,
		handler.FallbackGetUserById,
	))
}

// migrationWrap 基于泛型的通用路由包装器 (Generics Handler)
func migrationWrap[T any](
	migrationKey string,
	paramExtractor func(*T) map[string]interface{},
	functions ...func(ctx *gin.Context) (interface{}, error),
) gin.HandlerFunc {

	// 适配器：将强类型的业务函数适配为 SDK 的 migration.Function
	adapt := func(f func(ctx *gin.Context) (interface{}, error)) migration.Function {
		return func(args ...interface{}) (interface{}, error) {
			ctx := args[0].(*gin.Context)
			return f(ctx)
		}
	}

	var adaptedFunctions []migration.Function
	for _, f := range functions {
		adaptedFunctions = append(adaptedFunctions, adapt(f))
	}

	// 适配参数提取器
	paramHandler := func(args ...interface{}) map[string]interface{} {
		req := args[1].(*T)
		if paramExtractor != nil {
			return paramExtractor(req)
		}
		return map[string]interface{}{}
	}

	// 将当前 API 路由和新旧等函数一同注册到 SDK 的 Client Wrapper 中
	// 前三个分别为 oldFunc, newFunc, fallbackFunc
	wrap := client.Wrap(migrationKey, paramHandler, nil, adaptedFunctions...)

	// 返回标准的 Gin Handler
	return func(c *gin.Context) {
		var req T
		// 根据 HTTP 途径绑定 Request (支持 Query, JSON Body)
		if err := c.ShouldBind(&req); err != nil {
			c.JSON(400, gin.H{"code": 400, "message": "Bad Request"})
			return
		}
		// 覆盖绑定 URI，解决类似 /:id 的解析
		_ = c.ShouldBindUri(&req)

		// 触发主流程包装：将 gin 的原生请求 Context 与模型数据 req 传给内部各阶段策略
		res, err := wrap(c.Request.Context(), &req)
		if err != nil {
			c.JSON(500, gin.H{"code": 500, "message": err.Error()})
			return
		}

		// 内部策略处理后的有效业务响应统一在此吐出，彻底解决新老接口抢占 Write Header 和竞争写的死锁问题。
		c.JSON(200, gin.H{
			"code":    200,
			"message": "OK",
			"data":    res,
		})
	}
}
