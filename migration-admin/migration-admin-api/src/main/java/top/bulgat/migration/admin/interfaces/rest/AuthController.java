package top.bulgat.migration.admin.interfaces.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import top.bulgat.common.exception.BizException;
import top.bulgat.common.exception.ErrorCode;
import top.bulgat.common.model.Result;
import top.bulgat.migration.admin.infrastructure.config.AuthProperties;
import top.bulgat.migration.admin.infrastructure.config.JwtTokenProvider;
import top.bulgat.migration.admin.interfaces.dto.auth.LoginRequest;
import top.bulgat.migration.admin.interfaces.dto.auth.LoginResponse;
import top.bulgat.migration.admin.interfaces.dto.auth.UserInfo;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthProperties authProperties;
    private final JwtTokenProvider tokenProvider;

    public AuthController(AuthProperties authProperties, JwtTokenProvider tokenProvider) {
        this.authProperties = authProperties;
        this.tokenProvider = tokenProvider;
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        if (!authProperties.getUsername().equals(request.username()) ||
                !authProperties.getPassword().equals(request.password())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "username or password incorrect");
        }

        String token = tokenProvider.generateToken(request.username());
        UserInfo userInfo = new UserInfo(authProperties.getUsername(), authProperties.getDisplayName());
        return Result.success(new LoginResponse(token, userInfo));
    }

    @GetMapping("/query_current_user")
    public Result<UserInfo> queryCurrentUser(HttpServletRequest request) {
        String token = parseJwt(request);
        if (token == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "unauthorized");
        }
        String username = tokenProvider.getUsernameFromToken(token);
        if (!authProperties.getUsername().equals(username)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "unauthorized");
        }

        return Result.success(new UserInfo(authProperties.getUsername(), authProperties.getDisplayName()));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success(null);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}
