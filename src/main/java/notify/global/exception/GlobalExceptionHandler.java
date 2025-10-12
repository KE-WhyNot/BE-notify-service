package notify.global.exception;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import notify.global.common.BaseResponse;
import notify.global.exception.code.status.GlobalErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.validation.BindException;


@Slf4j
@RestControllerAdvice
@Hidden
public class GlobalExceptionHandler {

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResourceFound(Exception e, HttpServletRequest req) {
        log.debug("Static resource not found [{} {}]: {}", req.getMethod(), req.getRequestURI(), e.getMessage());
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(RestApiException.class)
    public ResponseEntity<BaseResponse<Void>> handleRestApi(RestApiException e) {
        return BaseResponse.fail(e.getCode());
    }

    @ExceptionHandler({ MethodArgumentNotValidException.class, BindException.class })
    public ResponseEntity<BaseResponse<Void>> handleValidation(Exception e, HttpServletRequest req) {
        logWarn(e, req);
        return BaseResponse.fail(GlobalErrorCode.INVALID_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleEtc(Exception e, HttpServletRequest req) {
        logError(e, req);
        return BaseResponse.fail(GlobalErrorCode.INTERNAL_SERVER_ERROR);
    }

    private void logWarn(Exception e, HttpServletRequest req) {
        log.warn("Validation Error [{} {}]: {}", req.getMethod(), req.getRequestURI(), e.getMessage());
    }
    private void logError(Exception e, HttpServletRequest req) {
        log.error("Unhandled Error [{} {}]", req.getMethod(), req.getRequestURI(), e);
    }
}