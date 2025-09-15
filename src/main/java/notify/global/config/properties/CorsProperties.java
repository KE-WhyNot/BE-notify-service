package notify.global.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration

public class CorsProperties {
    private String[] allowedOrigins = {"http://localhost:3000"};
    private String[] allowedMethods = {"GET","POST","PATCH","DELETE","OPTIONS"};
    private String[] allowedHeaders = {"*"};
    private boolean allowCredentials = true;
}
