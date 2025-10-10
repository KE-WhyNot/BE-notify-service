package notify.global.exception;

import lombok.Getter;
import notify.global.exception.code.BaseCode;

@Getter
public class RestApiException extends RuntimeException {
    private final BaseCode code;

    public RestApiException(BaseCode code) {
        super(code.getMessage());
        this.code = code;
    }
}
