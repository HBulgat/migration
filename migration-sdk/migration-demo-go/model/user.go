package model

type GetUserByIdRequest struct {
	UserID string `json:"user_id" form:"user_id" uri:"id"`
}

type GetUserByIdResponse struct {
	UserID     string `json:"user_id"`
	Username   string `json:"username"`
	UserAvatar string `json:"user_avatar"`
}
