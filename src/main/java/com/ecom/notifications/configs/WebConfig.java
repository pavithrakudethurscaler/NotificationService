package com.ecom.notifications.configs;

import com.ecom.notifications.auth.AuthInterceptor;
import com.ecom.notifications.auth.InternalApiInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final AuthInterceptor authInterceptor;
    private final InternalApiInterceptor internalApiInterceptor;

    public WebConfig(AuthInterceptor authInterceptor, InternalApiInterceptor internalApiInterceptor) {
        this.authInterceptor = authInterceptor;
        this.internalApiInterceptor = internalApiInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Customer endpoints: user login token
        registry.addInterceptor(authInterceptor).addPathPatterns("/api/v1/notifications", "/api/v1/notifications/**");
        // Event intake from other services: shared internal token
        registry.addInterceptor(internalApiInterceptor).addPathPatterns("/internal/**");
    }
}
