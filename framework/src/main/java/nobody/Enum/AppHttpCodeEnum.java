package nobody.Enum;

public enum AppHttpCodeEnum {
    SUCCESS(200, "ok"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    LOGIN_ERROR(401, "用户名或密码错误"),
    ACCOUNT_DISABLED(403, "账号不可用"),
    BIZ_ERROR(409, "业务处理失败"),
    SYSTEM_ERROR(500, "系统异常");

    private final int code;
    private final String msg;

    AppHttpCodeEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
