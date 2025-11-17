package com.medicine.config;

import com.medicine.interceptor.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/login",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/files/**",
                        "/manifest.json",
                        "/service-worker.js",
                        "/icons/**"
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/");
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/");
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");
        registry.addResourceHandler("/icons/**")
                .addResourceLocations("classpath:/static/icons/");

        // PWA 관련 파일들을 정적 리소스로 제공
        registry.addResourceHandler("/manifest.json")
                .addResourceLocations("classpath:/static/manifest.json");
        registry.addResourceHandler("/service-worker.js")
                .addResourceLocations("classpath:/static/service-worker.js");
    }
}
