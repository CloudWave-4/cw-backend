package com.cjolivenetworks.CloudWave_Backend;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry){
        registry.addMapping("/*")
                .allowedOrigins(
                        "http://localhost:8080",
                        "https://olive4.click",
                        "http://3.35.133.58"
                )
                .allowedMethods("PATCH","GET","POST","PUT","DELETE","OPTIONS")
                .allowedHeaders("")
                .maxAge(3600);
    }
}
