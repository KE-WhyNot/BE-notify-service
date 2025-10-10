package notify.global.exception.code.status;

import lombok.AllArgsConstructor;
import notify.global.exception.code.BaseCode;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
public enum NotificationErrorCode implements BaseCode {
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION_404", "알림을 찾을 수 없습니다."),
    INVALID_USER_ID(HttpStatus.BAD_REQUEST, "NOTIFICATION_400", "유효하지 않은 사용자 ID입니다."),
    ALREADY_READ(HttpStatus.BAD_REQUEST, "NOTIFICATION_400", "이미 읽은 알림입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    @Override public HttpStatus getHttpStatus() { return status; }
    @Override public String getCode() { return code; }
    @Override public String getMessage() { return message; }
}
