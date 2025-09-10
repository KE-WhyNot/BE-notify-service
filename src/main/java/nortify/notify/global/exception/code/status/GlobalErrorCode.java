package nortify.notify.global.exception.code.status;

import lombok.AllArgsConstructor;
import nortify.notify.global.exception.code.BaseCode;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
public enum GlobalErrorCode implements BaseCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_400", "잘못된 요청입니다."),
    UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, "COMMON_401", "인증이 필요합니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_404", "리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500", "서버 오류입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    @Override public HttpStatus getHttpStatus() { return status; }
    @Override public String getCode() { return code; }
    @Override public String getMessage() { return message; }
}
