package nobody.Controller;

import nobody.domain.entity.ResponseResult;
import nobody.domain.service.LoginService;
import nobody.domain.service.AdminUserService;
import nobody.dto.Login.LoginResponseDto;
import nobody.dto.admin.AdminUserDtos;
import nobody.utils.RedisCache;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final LoginService loginService;
    private final RedisCache redisCache;
    private final AdminUserService adminUserService;

    public AdminAuthController(LoginService loginService, RedisCache redisCache, AdminUserService adminUserService) {
        this.loginService = loginService;
        this.redisCache = redisCache;
        this.adminUserService = adminUserService;
    }

    @PostMapping("/login")
    public ResponseResult<LoginResponseDto> login(@RequestBody Map<String, Object> req) {
        String nickname = req == null ? null : String.valueOf(req.getOrDefault("nickname", ""));
        String password = req == null ? null : String.valueOf(req.getOrDefault("password", ""));
        return loginService.login(nickname, password);
    }

    @PostMapping("/logout")
    public ResponseResult<Void> logout() {
        Long userId = currentUserId();
        if (userId != null) {
            redisCache.deleteObject("login:" + userId);
        }
        return ResponseResult.okResult();
    }

    @GetMapping("/me")
    public ResponseResult<Map<String, Object>> me() {
        Long userId = currentUserId();
        if (userId == null) {
            return ResponseResult.errorResult(401, "未登录");
        }

        Object userInfo = redisCache.getCacheObject("login:" + userId);
        if (userInfo == null) {
            return ResponseResult.errorResult(401, "登录状态无效");
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", userId);
        data.put("user", userInfo);
        data.put("roles", List.of("ADMIN"));
        return ResponseResult.okResult(data);
    }

    @GetMapping("/permissions")
    public ResponseResult<List<String>> permissions() {
        Long userId = currentUserId();
        if (userId == null) {
            return ResponseResult.errorResult(401, "未登录");
        }

        ResponseResult<AdminUserDtos.UserItemDto> detail = adminUserService.detail(userId);
        List<String> roleCodes = detail.getData() == null ? List.of() : detail.getData().getRoles();
        List<String> perms = mapPermissions(roleCodes);
        return ResponseResult.okResult(perms);
    }

    private List<String> mapPermissions(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return List.of("dashboard:read");
        }
        if (roles.contains("ADMIN")) {
            return List.of(
                    "dashboard:read",
                    "post:create",
                    "post:update",
                    "post:publish",
                    "post:offline",
                    "comment:read",
                    "comment:reply",
                    "comment:audit",
                    "comment:delete",
                    "user:read",
                    "user:update"
            );
        }
        // 非管理员按角色聚合
        List<String> perms = new java.util.ArrayList<>(List.of("dashboard:read"));
        if (roles.contains("EDITOR") || roles.contains("AUTHOR")) {
            perms.add("post:create");
            perms.add("post:update");
        }
        if (roles.contains("REVIEWER")) {
            perms.add("comment:read");
            perms.add("comment:reply");
            perms.add("comment:audit");
        }
        return perms.stream().distinct().toList();
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(authentication.getPrincipal()));
        } catch (Exception ignored) {
            return null;
        }
    }
}
