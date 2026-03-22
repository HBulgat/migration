package handler

import (
	"context"

	"github.com/HBulgat/migration-demo-go/model"
)

func NewGetUserById(ctx context.Context, req *model.GetUserByIdRequest) (interface{}, error) {
	return &model.GetUserByIdResponse{
		UserID:     req.UserID,
		Username:   "new_username" + req.UserID,
		UserAvatar: "new_user_avatar" + req.UserID,
	}, nil
}

func FallbackGetUserById(ctx context.Context, req *model.GetUserByIdRequest) (interface{}, error) {
	return &model.GetUserByIdResponse{
		UserID:     "fallback_user_id",
		Username:   "fallback_username",
		UserAvatar: "fallback_user_avatar",
	}, nil
}

func OldGetUserById(ctx context.Context, req *model.GetUserByIdRequest) (interface{}, error) {
	return &model.GetUserByIdResponse{
		UserID:     req.UserID,
		Username:   "username" + req.UserID,
		UserAvatar: "user_avatar" + req.UserID,
	}, nil
}

func UserParamHandler(req *model.GetUserByIdRequest) map[string]interface{} {
	return map[string]interface{}{
		"userId": req.UserID,
	}
}
