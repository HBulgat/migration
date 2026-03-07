package top.bulgat.migration.admin.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class JwtSecurityInterceptorTest {

    @Test
    void preHandle_shouldAllowInternalTokenOnSdkReadEndpoint() throws Exception {
        JwtTokenProvider tokenProvider = mock(JwtTokenProvider.class);
        JwtSecurityInterceptor interceptor = new JwtSecurityInterceptor(tokenProvider);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/migration_task/query");
        request.addHeader("X-Internal-Token", "internal-token");

        boolean allowed = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertFalse(allowed);
    }

    @Test
    void preHandle_shouldRejectInternalTokenOnAdminWriteEndpoint() throws Exception {
        JwtTokenProvider tokenProvider = mock(JwtTokenProvider.class);
        JwtSecurityInterceptor interceptor = new JwtSecurityInterceptor(tokenProvider);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/migration_task/create");
        request.addHeader("X-Internal-Token", "internal-token");

        boolean allowed = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertFalse(allowed);
    }

    @Test
    void preHandle_shouldAllowValidJwtOnAdminWriteEndpoint() throws Exception {
        JwtTokenProvider tokenProvider = mock(JwtTokenProvider.class);
        when(tokenProvider.validateToken("jwt-token")).thenReturn(true);
        JwtSecurityInterceptor interceptor = new JwtSecurityInterceptor(tokenProvider);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/migration_task/create");
        request.addHeader("Authorization", "Bearer jwt-token");

        boolean allowed = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertTrue(allowed);
    }
}
