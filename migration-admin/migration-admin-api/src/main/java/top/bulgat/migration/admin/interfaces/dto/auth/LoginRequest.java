package top.bulgat.migration.admin.interfaces.dto.auth;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

public record LoginRequest(
        @NotBlank(message = "username is required")
        @Length(max = 64, message = "username length must not exceed 64")
        String username,

        @NotBlank(message = "password is required")
        @Length(max = 128, message = "password length must not exceed 128")
        String password
) {
}
