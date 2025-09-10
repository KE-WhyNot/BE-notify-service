package nortify.notify.global.exception;

import lombok.Getter;
import nortify.notify.global.exception.code.BaseCode;

@Getter
public class RestApiException extends RuntimeException {
    private final BaseCode code;

    public RestApiException(BaseCode code) {
        super(code.getMessage());
        this.code = code;
    }
}
