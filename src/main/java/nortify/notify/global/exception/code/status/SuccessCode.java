package nortify.notify.global.exception.code.status;

import lombok.AllArgsConstructor;
import nortify.notify.global.exception.code.BaseCode;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
public enum SuccessCode implements BaseCode {
    OK(HttpStatus.OK, "200OK", "요청에 성공하였습니다."),
    CREATED(HttpStatus.CREATED, "201CREATED", "리소스가 생성되었습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    @Override public HttpStatus getHttpStatus() { return status; }
    @Override public String getCode() { return code; }
    @Override public String getMessage() { return message; }
}
