package com.cjolivenetworks.CloudWave_Backend.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry){
        registry.addMapping("/**")
                .allowedOrigins(
                        "http://localhost:3000",
                        "https://olive4.click",
                        "http://3.35.133.58:3000",
                        "http://cloudwave-front-static.s3-website.ap-northeast-2.amazonaws.com"
                )
                .allowedMethods("PATCH","GET","POST","PUT","DELETE","OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
