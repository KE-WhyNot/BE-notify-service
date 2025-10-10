package notify.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {
    
    @Value("${server.port:8083}")
    private String serverPort;
    
    @Value("${spring.profiles.active:local}")
    private String activeProfile;
    
    @Bean
    public OpenAPI openAPI() {
        OpenAPI openAPI = new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("UserIdHeader",
                                new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER).name("X-User-Id")
                                        .description("게이트웨이가 검증 후 전달한 사용자 ID")))
                .addSecurityItem(new SecurityRequirement()
                        .addList("GatewaySignature").addList("UserIdHeader"))
                .info(new Info().title("Notify API").version("v1")
                        .description("알림 생성(내부) + 알림함 조회/읽음(사용자)"));
        
        // 환경별 서버 설정
        if ("prod".equals(activeProfile)) {
            openAPI.servers(List.of(
                    new Server().url("https://notify.youth-fi.com").description("Production Server")
            ));
        } else if ("dev".equals(activeProfile)) {
            openAPI.servers(List.of(
                    new Server().url("https://notify.youth-fi.com").description("Development Server"),
                    new Server().url("http://localhost:" + serverPort).description("Local Server")
            ));
        } else {
            // local 또는 기본값
            openAPI.servers(List.of(
                    new Server().url("https://notify.youth-fi.com").description("Production Server"),
                    new Server().url("http://localhost:" + serverPort).description("Local Server")
            ));
        }
        
        return openAPI;
    }
}

