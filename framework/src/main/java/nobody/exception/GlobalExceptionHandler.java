package nobody.exception;

import nobody.Enum.AppHttpCodeEnum;
import nobody.domain.entity.ResponseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseResult<Void> handleBusinessException(BusinessException ex) {
        return ResponseResult.errorResult(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseResult<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String msg = fieldError == null ? AppHttpCodeEnum.BAD_REQUEST.getMsg() : fieldError.getDefaultMessage();
        return ResponseResult.errorResult(AppHttpCodeEnum.BAD_REQUEST.getCode(), msg);
    }

    @ExceptionHandler(BindException.class)
    public ResponseResult<Void> handleBindException(BindException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String msg = fieldError == null ? AppHttpCodeEnum.BAD_REQUEST.getMsg() : fieldError.getDefaultMessage();
        return ResponseResult.errorResult(AppHttpCodeEnum.BAD_REQUEST.getCode(), msg);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseResult<Void> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        return ResponseResult.errorResult(AppHttpCodeEnum.BAD_REQUEST.getCode(), "缺少参数: " + ex.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseResult<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        return ResponseResult.errorResult(AppHttpCodeEnum.BAD_REQUEST.getCode(), "参数类型错误: " + ex.getName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseResult<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        return ResponseResult.errorResult(AppHttpCodeEnum.BAD_REQUEST.getCode(), "请求体格式错误");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseResult<Void> handleAccessDeniedException(AccessDeniedException ex) {
        return ResponseResult.errorResult(AppHttpCodeEnum.FORBIDDEN.getCode(), AppHttpCodeEnum.FORBIDDEN.getMsg());
    }

    @ExceptionHandler(Exception.class)
    public ResponseResult<Void> handleException(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseResult.errorResult(AppHttpCodeEnum.SYSTEM_ERROR.getCode(), AppHttpCodeEnum.SYSTEM_ERROR.getMsg());
    }
}
