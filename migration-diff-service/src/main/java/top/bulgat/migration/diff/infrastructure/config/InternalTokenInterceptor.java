package top.bulgat.migration.diff.infrastructure.config;

import com.alibaba.fastjson2.JSON;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import top.bulgat.common.base.exception.ErrorCode;
import top.bulgat.common.base.model.Result;

@Component
public class InternalTokenInterceptor implements HandlerInterceptor {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private final InternalTokenProperties properties;

    public InternalTokenInterceptor(InternalTokenProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String token = request.getHeader(INTERNAL_TOKEN_HEADER);
        if (properties.getInternalToken().equals(token)) {
            return true;
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
