package top.bulgat.migration.admin.interfaces.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserInfo(
        @JsonProperty("username") String username,
        @JsonProperty("display_name") String displayName
) {
}
