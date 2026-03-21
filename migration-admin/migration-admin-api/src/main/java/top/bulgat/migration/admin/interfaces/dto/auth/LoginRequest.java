package top.bulgat.migration.admin.interfaces.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

@Schema(description = "登录请求")
public record LoginRequest(
        @Schema(description = "用户名")
        @NotBlank(message = "username is required")
        @Length(max = 64, message = "username length must not exceed 64")
        String username,

        @Schema(description = "密码")
        @NotBlank(message = "password is required")
        @Length(max = 128, message = "password length must not exceed 128")
        String password
) {
}
