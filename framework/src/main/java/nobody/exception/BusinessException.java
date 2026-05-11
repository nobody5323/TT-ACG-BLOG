package nobody.exception;

import nobody.Enum.AppHttpCodeEnum;

public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(AppHttpCodeEnum codeEnum) {
        super(codeEnum.getMsg());
        this.code = codeEnum.getCode();
    }

    public BusinessException(AppHttpCodeEnum codeEnum, String message) {
        super(message);
        this.code = codeEnum.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
