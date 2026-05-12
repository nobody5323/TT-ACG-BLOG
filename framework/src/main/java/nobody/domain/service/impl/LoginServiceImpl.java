package nobody.domain.service.impl;

import nobody.Enum.AppHttpCodeEnum;
import nobody.domain.entity.ResponseResult;
import nobody.domain.mapper.SysUserMapper;
import nobody.domain.service.LoginService;
import nobody.dto.Login.LoginResponseDto;
import nobody.exception.BusinessException;
import nobody.utils.JwtUtil;
import nobody.utils.RedisCache;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class LoginServiceImpl implements LoginService {

    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();
    private static final String DEFAULT_ROLE_NAME = "博主";

    private final SysUserMapper sysUserMapper;
    private final RedisCache redisCache;

    public LoginServiceImpl(SysUserMapper sysUserMapper, RedisCache redisCache) {
        this.sysUserMapper = sysUserMapper;
        this.redisCache = redisCache;
    }

    @Override
    public ResponseResult<LoginResponseDto> login(String nickname, String password) {
        if (nickname == null || nickname.isBlank() || password == null || password.isBlank()) {
            throw new BusinessException(AppHttpCodeEnum.BAD_REQUEST, "昵称和密码不能为空");
        }

        List<Map<String, Object>> rows = sysUserMapper.selectByNickname(nickname);
        if (rows == null || rows.isEmpty()) {
            throw new BusinessException(AppHttpCodeEnum.LOGIN_ERROR);
        }

        Map<String, Object> user = rows.get(0);
        validateUserForLogin(user);
        if (!PASSWORD_ENCODER.matches(password, asString(user.get("passwordHash")))) {
            throw new BusinessException(AppHttpCodeEnum.LOGIN_ERROR);
        }
        return buildLoginResult(user);
    }

    @Override
    public ResponseResult<LoginResponseDto> register(String nickname, String password) {
        if (nickname == null || nickname.isBlank() || password == null || password.isBlank()) {
            throw new BusinessException(AppHttpCodeEnum.BAD_REQUEST, "昵称和密码不能为空");
        }
        String normalizedNickname = nickname.trim();
        if (normalizedNickname.length() < 2 || normalizedNickname.length() > 24) {
            throw new BusinessException(AppHttpCodeEnum.BAD_REQUEST, "昵称长度需在 2-24 之间");
        }
        if (password.length() < 6 || password.length() > 64) {
            throw new BusinessException(AppHttpCodeEnum.BAD_REQUEST, "密码长度需在 6-64 之间");
        }

        Long exists = sysUserMapper.countByNickname(normalizedNickname);
        if (exists != null && exists > 0) {
            throw new BusinessException(AppHttpCodeEnum.BIZ_ERROR, "昵称已存在");
        }

        String hash = PASSWORD_ENCODER.encode(password);
        int created = sysUserMapper.insertUser(
                normalizedNickname,
                normalizedNickname,
                hash,
                "",
                1
        );
        if (created <= 0) {
            throw new BusinessException(AppHttpCodeEnum.SYSTEM_ERROR, "注册失败");
        }

        Long userId = sysUserMapper.lastInsertId();
        if (userId != null) {
            sysUserMapper.insertUserRoleByRoleName(userId, DEFAULT_ROLE_NAME);
        }

        List<Map<String, Object>> rows = sysUserMapper.selectByNickname(normalizedNickname);
        if (rows == null || rows.isEmpty()) {
            throw new BusinessException(AppHttpCodeEnum.SYSTEM_ERROR, "注册后用户读取失败");
        }
        return buildLoginResult(rows.get(0));
    }

    private ResponseResult<LoginResponseDto> buildLoginResult(Map<String, Object> user) {
        Long userId = asLong(user.get("id"));
        String token = JwtUtil.createJWT(String.valueOf(userId));
        String redisKey = "login:" + userId;

        LoginResponseDto.UserInfoDto userInfo = new LoginResponseDto.UserInfoDto();
        userInfo.setId(userId);
        userInfo.setNickname(asString(user.get("nickname")));
        userInfo.setLevel(asString(user.get("level")));
        userInfo.setSignature(asString(user.get("signature")));
        redisCache.setCacheObject(redisKey, userInfo, 24, TimeUnit.HOURS);

        return ResponseResult.okResult(new LoginResponseDto(token, userInfo));
    }

    private void validateUserForLogin(Map<String, Object> user) {
        Integer status = asInt(user.get("status"));
        Integer deleted = asInt(user.get("deleted"));
        if (status == null || status != 1 || (deleted != null && deleted != 0)) {
            throw new BusinessException(AppHttpCodeEnum.ACCOUNT_DISABLED);
        }
    }

    private String asString(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private Integer asInt(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number n) {
            return n.intValue();
        }
        return Integer.parseInt(String.valueOf(o));
    }

    private Long asLong(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(String.valueOf(o));
    }
}
