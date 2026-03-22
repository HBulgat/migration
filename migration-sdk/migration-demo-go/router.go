package main

import (
	"fmt"
	"net/http"

	"github.com/HBulgat/migration-demo-go/handler"
	"github.com/HBulgat/migration-sdk-go"
	"github.com/gin-gonic/gin"
)

// RegisterRoutes 统一注册所有的路由和迁移策略包装
func RegisterRoutes(r *gin.Engine) {
	// 注册用户查询接口
	r.GET("/api/v1/user", migrationWrap(
		"test-1111110101",        // 迁移策略 Key
		handler.UserParamHandler, // 参数提取器: func(ctx *gin.Context) map[string]interface{}
		nil,
		handler.OldGetUserById,      // 旧逻辑: func(ctx *gin.Context) (interface{}, error)
		handler.NewGetUserById,      // 新逻辑: func(ctx *gin.Context) (interface{}, error)
		handler.FallbackGetUserById, // 降级逻辑: func(ctx *gin.Context) (interface{}, error)
	))
}

// migrationWrap 通用路由包装器 (无泛型版本)
func migrationWrap(
	migrationKey string,
	paramExtractor func(ctx *gin.Context) map[string]interface{},
	postProcessor migration.PostProcessor,
	oldFunc func(ctx *gin.Context) (interface{}, error),
	newFunc func(ctx *gin.Context) (interface{}, error),
	fallbackFunc func(ctx *gin.Context) (interface{}, error),
) gin.HandlerFunc {
	adapt := func(f func(ctx *gin.Context) (interface{}, error)) migration.Function {
		return func(args ...interface{}) (interface{}, error) {
			if len(args) == 0 {
				return nil, fmt.Errorf("no context provided to migration function")
			}

			ctx, ok := args[0].(*gin.Context)
			if !ok {
				// 如果类型不对，返回明确错误而不是 Panic
				return nil, fmt.Errorf("invalid context type: expected *gin.Context, got %T", args[0])
			}

			return f(ctx)
		}
	}

	// 适配参数提取器
	paramHandler := func(args ...interface{}) map[string]interface{} {
		if len(args) == 0 {
			return map[string]interface{}{}
		}
		ctx, ok := args[0].(*gin.Context)
		if !ok {
			return map[string]interface{}{}
		}
		return paramExtractor(ctx)
	}

	sdkOld := adapt(oldFunc)
	sdkNew := adapt(newFunc)
	sdkFallback := adapt(fallbackFunc)

	wrap := client.Wrap(migrationKey, paramHandler, postProcessor, sdkOld, sdkNew, sdkFallback)

	// 返回最终的 Gin HTTP Handler
	return func(c *gin.Context) {
		res, err := wrap(c)

		if err != nil {
			// 根据业务需求处理错误，这里统一返回 500
			c.JSON(http.StatusInternalServerError, gin.H{
				"code":    http.StatusInternalServerError,
				"message": err.Error(),
			})
			return
		}

		// 统一成功响应格式
		c.JSON(http.StatusOK, gin.H{
			"code":    http.StatusOK,
			"message": "OK",
			"data":    res,
		})
	}
}
