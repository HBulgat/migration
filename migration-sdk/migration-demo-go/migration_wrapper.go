package main

import (
	"net/http"

	"github.com/HBulgat/migration-sdk-go"
	"github.com/gin-gonic/gin"
)

// HandleWithMigration 是一个高度通用的方法，适用于该 HTTP 服务下的任意迁移接口！
// 你不需要为每个接口写一遍迁移逻辑或者 handler 了，只需传入对应的 executeFn 和 参数解析闭包。
// T 代表期望响应的数据结构
func HandleWithMigration[T any](executeFn *migration.ExecuteFunction, paramExtractor func(c *gin.Context) []interface{}) gin.HandlerFunc {
	return func(c *gin.Context) {
		// 1. 抽取业务参数
		args := paramExtractor(c)

		// 2. 发起通用的迁移 SDK 调用（带入 Header 自动追踪 TraceId）
		res, err := executeFn.WithTraceId(c.GetHeader("X-Trace-Id")).Execute(args...)

		// 3. 通用的结果响应处理
		if err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{
				"code":    500,
				"message": err.Error(),
				"data":    res, // 即便是失败，也保留暴露给前端的兜底返回值
			})
			return
		}

		c.JSON(http.StatusOK, gin.H{
			"code":    0,
			"message": "success",
			"data":    res.(*T),
		})
	}
}
