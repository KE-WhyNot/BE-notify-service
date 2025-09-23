package notify.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("BearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT 토큰을 입력하세요. 예: Bearer <your-jwt-token>")))
                .addSecurityItem(new SecurityRequirement()
                        .addList("BearerAuth"))
                .info(new Info()
                        .title("Notify API")
                        .version("v1")
                        .description("알림 생성(내부) + 알림함 조회/읽음(사용자)"));
    }
}

