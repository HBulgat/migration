package top.bulgat.migration.admin.infrastructure.config;

import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import top.bulgat.common.base.exception.ErrorCode;
import top.bulgat.common.base.model.Result;
import java.io.IOException;

@Component
public class JwtSecurityInterceptor implements HandlerInterceptor {

    private final JwtTokenProvider tokenProvider;
    private final ObjectMapper objectMapper;
    private final String internalToken;

    public JwtSecurityInterceptor(JwtTokenProvider tokenProvider, ObjectMapper objectMapper,
            @org.springframework.beans.factory.annotation.Value("${migration.admin.internal-token:}") String internalToken) {
        this.tokenProvider = tokenProvider;
        this.objectMapper = objectMapper;
        this.internalToken = internalToken;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // 1. Check internal token for SDK M2M communications
        String headerInternalToken = request.getHeader("X-Internal-Token");
        if (internalToken != null && !internalToken.isEmpty() && internalToken.equals(headerInternalToken)) {
            return true;
        }

        // 2. Fallback to standard JWT check for Admin UI Users
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            if (tokenProvider.validateToken(token)) {
                return true;
            }
        }
        sendUnauthorizedResponse(response);
        return false;
    }

    private void sendUnauthorizedResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(JSON.toJSONString(Result.fail(ErrorCode.UNAUTHORIZED)));
    }
}
