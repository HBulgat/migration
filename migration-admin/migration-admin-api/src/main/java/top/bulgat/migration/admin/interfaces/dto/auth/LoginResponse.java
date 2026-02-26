package top.bulgat.migration.admin.interfaces.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LoginResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("user_info") UserInfo userInfo
) {
}
