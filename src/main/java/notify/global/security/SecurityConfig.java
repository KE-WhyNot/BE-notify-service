package notify.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final ObjectMapper om = new ObjectMapper();
    private final JwtUtil jwtUtil;

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
                .addFilterBefore(new JwtAuthenticationFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);

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


    static class JwtAuthenticationFilter extends OncePerRequestFilter {
        private static final String HDR_AUTHORIZATION = "Authorization";
        private static final String HDR_REQ_ID = "X-Request-Id";
        
        private final JwtUtil jwtUtil;

        public JwtAuthenticationFilter(JwtUtil jwtUtil) {
            this.jwtUtil = jwtUtil;
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

            String uri = req.getRequestURI();
            String method = req.getMethod();
            String client = req.getRemoteAddr();
            String authHeader = req.getHeader(HDR_AUTHORIZATION);

            log.info("[SEC] IN {} {} ip={} authHdr={} traceId={}",
                    method, uri, client, safe(authHeader), traceId);

            try {
                // JWT 토큰 인증 처리
                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    String token = jwtUtil.extractTokenFromHeader(authHeader);
                    
                    if (token != null && jwtUtil.validateToken(token) && !jwtUtil.isTokenExpired(token)) {
                        String userId = jwtUtil.extractUserId(token);
                        
                        if (userId != null) {
                            var auth = new UsernamePasswordAuthenticationToken(
                                    userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
                            );
                            SecurityContextHolder.getContext().setAuthentication(auth);
                            log.debug("[SEC] JWT Authenticated principal={} traceId={}", userId, traceId);
                        } else {
                            log.debug("[SEC] Invalid JWT token - no userId traceId={}", traceId);
                        }
                    } else {
                        log.debug("[SEC] No valid JWT token. Proceeding as anonymous. traceId={}", traceId);
                    }
                }

                chain.doFilter(req, res);

            } catch (Exception e) {
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
            if (v != null && v.startsWith("Bearer ")) {
                return "Bearer ***";
            }
            return v;
        }
    }
}
