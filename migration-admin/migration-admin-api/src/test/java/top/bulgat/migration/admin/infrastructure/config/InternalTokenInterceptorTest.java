package top.bulgat.migration.admin.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class InternalTokenInterceptorTest {

    @Test
    void preHandle_shouldAllowRequestWithMatchedInternalToken() throws Exception {
        InternalTokenProperties properties = new InternalTokenProperties();
        properties.setInternalToken("internal-token");
        InternalTokenInterceptor interceptor = new InternalTokenInterceptor(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/internal/sdk/grayscale_rule/list");
        request.addHeader("X-Internal-Token", "internal-token");

        boolean allowed = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertTrue(allowed);
    }

    @Test
    void preHandle_shouldRejectRequestWithoutMatchedInternalToken() throws Exception {
        InternalTokenProperties properties = new InternalTokenProperties();
        properties.setInternalToken("internal-token");
        InternalTokenInterceptor interceptor = new InternalTokenInterceptor(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/internal/sdk/grayscale_rule/list");

        boolean allowed = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertFalse(allowed);
    }
}
