package notify.global.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
@Setter
@ConfigurationProperties(prefix = "cors")
public class CorsProperties {
    private String[] allowedOrigins = {"http://localhost:3000"};
    private String[] allowedMethods = {"GET","POST","PATCH","DELETE","OPTIONS"};
    private String[] allowedHeaders = {"*"};
    private boolean allowCredentials = true;
}
