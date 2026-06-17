package com.lingotlow.backendcore.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    // Desabilitar completamente o mapeamento para /api/**
    // Isso evita que endpoints da API sejam confundidos com recursos estáticos
    registry
        .addResourceHandler("/api/**")
        .addResourceLocations("classpath:/static/")
        .setCachePeriod(0);
  }
}
