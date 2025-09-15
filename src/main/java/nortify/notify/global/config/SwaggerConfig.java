package nortify.notify.global.config;

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
                        .addSecuritySchemes("UserIdHeader",
                                new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER).name("X-User-Id")
                                        .description("게이트웨이가 검증 후 전달한 사용자 ID")))
                .addSecurityItem(new SecurityRequirement()
                        .addList("GatewaySignature").addList("UserIdHeader"))
                .info(new Info().title("Notify API").version("v1")
                        .description("알림 생성(내부) + 알림함 조회/읽음(사용자)"));
    }
}

