package nortify.notify.global.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import nortify.notify.global.exception.code.BaseCode;
import org.springframework.http.ResponseEntity;

import java.time.Instant;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseResponse<T> {
    private final String code;
    private final String message;
    private final T data;
    private final Instant timestamp;

    private BaseResponse(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = Instant.now();
    }

    public static <T> ResponseEntity<BaseResponse<T>> success(BaseCode successCode, T data) {
        return ResponseEntity.status(successCode.getHttpStatus())
                .body(new BaseResponse<>(successCode.getCode(), successCode.getMessage(), data));
    }

    public static ResponseEntity<BaseResponse<Void>> success(BaseCode successCode) {
        return ResponseEntity.status(successCode.getHttpStatus())
                .body(new BaseResponse<>(successCode.getCode(), successCode.getMessage(), null));
    }

    public static ResponseEntity<BaseResponse<Void>> fail(BaseCode errorCode) {
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(new BaseResponse<>(errorCode.getCode(), errorCode.getMessage(), null));
    }
}