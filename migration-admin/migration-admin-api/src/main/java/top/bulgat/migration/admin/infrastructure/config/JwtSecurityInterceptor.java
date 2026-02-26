package top.bulgat.migration.admin.infrastructure.config;

import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import top.bulgat.common.exception.ErrorCode;
import top.bulgat.common.model.Result;
import top.bulgat.common.util.JsonUtils;

import java.io.IOException;

@Component
public class JwtSecurityInterceptor implements HandlerInterceptor {

    private final JwtTokenProvider tokenProvider;
    private final ObjectMapper objectMapper;

    public JwtSecurityInterceptor(JwtTokenProvider tokenProvider, ObjectMapper objectMapper) {
        this.tokenProvider = tokenProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            if (tokenProvider.validateToken(token)) {
                return true;
            }
        }
        return true;
//        sendUnauthorizedResponse(response);
//        return false;
    }

    private void sendUnauthorizedResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(JSON.toJSONString(Result.fail(ErrorCode.UNAUTHORIZED)));
    }
}
