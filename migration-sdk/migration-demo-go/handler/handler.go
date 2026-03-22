package handler

import (
	"fmt"

	"github.com/HBulgat/migration-demo-go/model"
	"github.com/gin-gonic/gin"
)

// NewGetUserById 新逻辑实现
func NewGetUserById(ctx *gin.Context) (interface{}, error) {
	var req model.GetUserByIdRequest
	if err := ctx.ShouldBindQuery(&req); err != nil {
		return nil, fmt.Errorf("bind query failed: %w", err)
	}

	fmt.Printf("[NewGetUserById] req: %+v\n", req)

	return &model.GetUserByIdResponse{
		UserID:     req.UserID,
		Username:   "new_username_" + req.UserID,
		UserAvatar: "new_avatar_" + req.UserID,
	}, nil
}

// OldGetUserById 旧逻辑实现
func OldGetUserById(ctx *gin.Context) (interface{}, error) {
	var req model.GetUserByIdRequest
	if err := ctx.ShouldBindQuery(&req); err != nil {
		return nil, fmt.Errorf("bind query failed: %w", err)
	}

	fmt.Printf("[OldGetUserById] req: %+v\n", req)

	return &model.GetUserByIdResponse{
		UserID:     req.UserID,
		Username:   "old_username_" + req.UserID,
		UserAvatar: "old_avatar_" + req.UserID,
	}, nil
}

// FallbackGetUserById 降级逻辑实现
func FallbackGetUserById(ctx *gin.Context) (interface{}, error) {
	var req model.GetUserByIdRequest
	_ = ctx.ShouldBindQuery(&req) // 忽略错误

	fmt.Printf("[FallbackGetUserById] req: %+v (might be empty)\n", req)

	return &model.GetUserByIdResponse{
		UserID:     "fallback_id",
		Username:   "fallback_user",
		UserAvatar: "fallback_avatar.png",
	}, nil
}

// UserParamHandler 参数提取器 (供迁移策略使用)
func UserParamHandler(ctx *gin.Context) map[string]interface{} {
	var req model.GetUserByIdRequest

	if err := ctx.ShouldBindQuery(&req); err != nil {
		fmt.Printf("[UserParamHandler] bind error: %v, using empty params\n", err)
		return map[string]interface{}{}
	}

	fmt.Printf("[UserParamHandler] extracted params: %+v\n", req)

	return map[string]interface{}{
		"userId": req.UserID,
	}
}
