package handler

import (
	"github.com/HBulgat/migration-demo-go/model"
	"github.com/gin-gonic/gin"
)

func NewGetUserById(ctx *gin.Context) (interface{}, error) {
	var req model.GetUserByIdRequest
	if err := ctx.ShouldBindQuery(&req); err != nil {
		return nil, err
	}
	return &model.GetUserByIdResponse{
		UserID:     req.UserID,
		Username:   "new_username" + req.UserID,
		UserAvatar: "new_user_avatar" + req.UserID,
	}, nil
}

func FallbackGetUserById(ctx *gin.Context) (interface{}, error) {
	var req model.GetUserByIdRequest
	if err := ctx.ShouldBindQuery(&req); err != nil {
		return nil, err
	}
	return &model.GetUserByIdResponse{
		UserID:     "fallback_user_id",
		Username:   "fallback_username",
		UserAvatar: "fallback_user_avatar",
	}, nil
}

func OldGetUserById(ctx *gin.Context) (interface{}, error) {
	var req model.GetUserByIdRequest
	if err := ctx.ShouldBindQuery(&req); err != nil {
		return nil, err
	}
	return &model.GetUserByIdResponse{
		UserID:     req.UserID,
		Username:   "username" + req.UserID,
		UserAvatar: "user_avatar" + req.UserID,
	}, nil
}

func UserParamHandler(ctx *gin.Context) map[string]interface{} {
	var req model.GetUserByIdRequest
	if err := ctx.ShouldBindQuery(&req); err != nil {
		return nil
	}
	return map[string]interface{}{
		"userId": req.UserID,
	}
}
