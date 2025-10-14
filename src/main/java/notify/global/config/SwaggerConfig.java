package notify.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
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
        // X-User-Id 헤더(apiKey) 스킴 정의
        SecurityScheme xUserIdScheme = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name("X-User-Id")
                .description("게이트웨이(ingress/auth)가 검증 후 전달하는 로그인 사용자 ID (문자열)");

        // 기본 OpenAPI 빌더
        OpenAPI openAPI = new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("UserIdHeader", xUserIdScheme))
                // 전역 보안 요구사항: X-User-Id
                .addSecurityItem(new SecurityRequirement().addList("UserIdHeader"))
                .info(new Info()
                        .title("Notify API")
                        .version("v1")
                        .description("알림 생성(내부) + 알림함 조회/읽음(사용자)"));

        // 환경별 서버 URL
        if ("prod".equalsIgnoreCase(activeProfile)) {
            openAPI.servers(List.of(
                    new Server().url("https://notify.youth-fi.com").description("Production")
            ));
        } else if ("dev".equalsIgnoreCase(activeProfile)) {
            openAPI.servers(List.of(
                    new Server().url("https://notify.youth-fi.com").description("Development"),
                    new Server().url("http://localhost:" + serverPort).description("Local")
            ));
        } else { // local or default
            openAPI.servers(List.of(
                    new Server().url("http://localhost:" + serverPort).description("Local")
            ));
        }

        return openAPI;
        }
}
