package notify.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import notify.global.exception.code.status.GlobalErrorCode;
import org.slf4j.MDC;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.List;

@Configuration
@Slf4j
public class SecurityConfig {

    private final ObjectMapper om = new ObjectMapper();

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(c -> c.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/actuator/**").permitAll()
                        .requestMatchers("/api/internal/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                )
                .addFilterBefore(new UserIdHeaderFilter(authenticationEntryPoint()), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 401
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (req, res, ex) -> writeError(res, GlobalErrorCode.UNAUTHORIZED_ACCESS, req.getRequestURI());
    }

    // 403
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (req, res, ex) -> writeError(res, GlobalErrorCode.INVALID_REQUEST, req.getRequestURI());
        // 필요하면 별도 ERROR CODE 분리 (e.g. FORBIDDEN_403)
    }

    private void writeError(HttpServletResponse res, GlobalErrorCode code, String path) throws IOException {
        res.setStatus(code.getHttpStatus().value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        var body = Map.of(
                "isSuccess", false,
                "code", code.getCode(),
                "message", code.getMessage(),
                "path", path,
                "timestamp", OffsetDateTime.now().toString(),
                "traceId", MDC.get("traceId")
        );
        res.getWriter().write(om.writeValueAsString(body));
    }

    /**
     * Ingress가 셋팅한 X-User-Id 헤더로 인증 컨텍스트 구성 + 보안 로깅
     */
    static class UserIdHeaderFilter extends OncePerRequestFilter {
        private static final String HDR_USER_ID = "X-User-Id";
        private static final String HDR_REQ_ID  = "X-Request-Id";

        private final AuthenticationEntryPoint entryPoint;

        UserIdHeaderFilter(AuthenticationEntryPoint entryPoint) {
            this.entryPoint = entryPoint;
        }

        @Override
        protected boolean shouldNotFilter(HttpServletRequest request) {
            // CORS 프리플라이트는 스킵
            return "OPTIONS".equalsIgnoreCase(request.getMethod());
        }

        @Override
        protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                throws ServletException, IOException {

            String traceId = req.getHeader(HDR_REQ_ID);
            if (traceId == null || traceId.isBlank()) traceId = UUID.randomUUID().toString();
            MDC.put("traceId", traceId);
            res.setHeader(HDR_REQ_ID, traceId);

            long startNs = System.nanoTime();

            //테스트용
            String uri = req.getRequestURI();
            String method = req.getMethod();
            String client = req.getRemoteAddr();
            String userIdHeader = req.getHeader(HDR_USER_ID);

            log.info("[SEC] IN {} {} ip={} userIdHdr={} traceId={}",
                    method, uri, client, safe(userIdHeader), traceId);

            try {
                // /api/** 에서는 X-User-Id 필수
                boolean apiPath = uri.startsWith("/api/");

                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    if (userIdHeader != null && !userIdHeader.isBlank()) {
                        var auth = new UsernamePasswordAuthenticationToken(
                            userIdHeader, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        );
                        SecurityContextHolder.getContext().setAuthentication(auth);
                        log.debug("[SEC] Authenticated principal={} traceId={}", userIdHeader, traceId);
                    } else if (apiPath) {
                        // 인증이 필요한 경로인데 헤더가 없으면 401
                        entryPoint.commence(req, res, null);
                        return;
                    } else {
                        log.debug("[SEC] No X-User-Id header (non-API path). Proceeding anonymous. traceId={}", traceId);
                    }
                }

                chain.doFilter(req, res);

            } catch (Exception e) {
                // 예외
                log.error("[SEC] Exception in filter: {} traceId={}", e.toString(), traceId);
                throw e;
            } finally {
                long tookMs = (System.nanoTime() - startNs) / 1_000_000;
                int status = res.getStatus();
                log.info("[SEC] OUT {} {} status={} took={}ms traceId={}",
                        method, uri, status, tookMs, traceId);

                MDC.remove("traceId");
            }
        }

        private String safe(String v) {
            // 민감값 마스킹,정규화..필요시..
            return v;
        }
    }
}
