package top.bulgat.migration.admin.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtSecurityInterceptor jwtSecurityInterceptor;
    private final InternalTokenInterceptor internalTokenInterceptor;

    public WebMvcConfig(
            JwtSecurityInterceptor jwtSecurityInterceptor,
            InternalTokenInterceptor internalTokenInterceptor) {
        this.jwtSecurityInterceptor = jwtSecurityInterceptor;
        this.internalTokenInterceptor = internalTokenInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtSecurityInterceptor)
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns("/api/v1/auth/login")
                .excludePathPatterns("/api/internal/sdk/**")
                .excludePathPatterns("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/doc.html", "/webjars/**");
        registry.addInterceptor(internalTokenInterceptor)
                .addPathPatterns("/api/internal/sdk/**")
                .excludePathPatterns("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/doc.html", "/webjars/**");
    }
}
