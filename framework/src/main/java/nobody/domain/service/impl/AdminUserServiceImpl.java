package nobody.domain.service.impl;

import nobody.Enum.AppHttpCodeEnum;
import nobody.domain.entity.ResponseResult;
import nobody.domain.mapper.AdminUserMapper;
import nobody.domain.service.AdminUserService;
import nobody.dto.admin.AdminUserDtos;
import nobody.exception.BusinessException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();
    private final AdminUserMapper adminUserMapper;

    public AdminUserServiceImpl(AdminUserMapper adminUserMapper) {
        this.adminUserMapper = adminUserMapper;
    }

    @Override
    public ResponseResult<AdminUserDtos.UserListResponseDto> list(String keyword, Integer status, Long pageNum, Integer pageSize) {
        long current = pageNum == null || pageNum <= 0 ? 1 : pageNum;
        int size = pageSize == null || pageSize <= 0 ? 10 : Math.min(pageSize, 100);
        long offset = (current - 1) * size;

        Long total = adminUserMapper.countUsers(keyword, status);
        List<Map<String, Object>> rows = adminUserMapper.selectUsers(keyword, status, offset, size);
        List<AdminUserDtos.UserItemDto> items = rows.stream().map(this::toDto).toList();

        AdminUserDtos.UserListResponseDto dto = new AdminUserDtos.UserListResponseDto();
        dto.setTotal(total == null ? 0L : total);
        dto.setItems(items);
        return ResponseResult.okResult(dto);
    }

    @Override
    public ResponseResult<AdminUserDtos.UserItemDto> detail(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(AppHttpCodeEnum.BAD_REQUEST, "用户ID非法");
        }
        Map<String, Object> row = adminUserMapper.selectUserById(id);
        if (row == null || row.isEmpty()) {
            throw new BusinessException(AppHttpCodeEnum.NOT_FOUND, "用户不存在");
        }
        return ResponseResult.okResult(toDto(row));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseResult<AdminUserDtos.UserItemDto> create(AdminUserDtos.SaveUserRequestDto req) {
        validateSave(req, true);
        String hash = PASSWORD_ENCODER.encode(req.getPassword());
        int affected = adminUserMapper.insertUser(req.getUsername(), req.getNickname(), hash, req.getSignature(), normalizeStatus(req.getStatus()));
        if (affected <= 0) {
            throw new BusinessException(AppHttpCodeEnum.SYSTEM_ERROR, "创建用户失败");
        }
        Long id = adminUserMapper.lastInsertId();
        return detail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseResult<AdminUserDtos.UserItemDto> update(Long id, AdminUserDtos.SaveUserRequestDto req) {
        if (id == null || id <= 0) {
            throw new BusinessException(AppHttpCodeEnum.BAD_REQUEST, "用户ID非法");
        }
        validateSave(req, false);
        int affected = adminUserMapper.updateUser(id, req.getUsername(), req.getNickname(), req.getSignature(), normalizeStatus(req.getStatus()));
        if (affected <= 0) {
            throw new BusinessException(AppHttpCodeEnum.NOT_FOUND, "用户不存在");
        }
        return detail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseResult<Void> patchRoles(Long id, AdminUserDtos.PatchRolesRequestDto req) {
        if (id == null || id <= 0) {
            throw new BusinessException(AppHttpCodeEnum.BAD_REQUEST, "用户ID非法");
        }
        if (req == null || req.getRoles() == null || req.getRoles().isEmpty()) {
            throw new BusinessException(AppHttpCodeEnum.BAD_REQUEST, "角色列表不能为空");
        }

        adminUserMapper.deleteUserRolesByUserId(id);
        for (String role : req.getRoles()) {
            if (role == null || role.isBlank()) continue;
            adminUserMapper.insertUserRoleByCode(id, normalizeRoleName(role));
        }
        return ResponseResult.okResult();
    }

    @Override
    public ResponseResult<Void> patchStatus(Long id, AdminUserDtos.PatchStatusRequestDto req) {
        if (id == null || id <= 0) {
            throw new BusinessException(AppHttpCodeEnum.BAD_REQUEST, "用户ID非法");
        }
        if (req == null || (req.getStatus() != 0 && req.getStatus() != 1)) {
            throw new BusinessException(AppHttpCodeEnum.BAD_REQUEST, "状态只允许 0/1");
        }
        int affected = adminUserMapper.updateUserStatus(id, req.getStatus());
        if (affected <= 0) {
            throw new BusinessException(AppHttpCodeEnum.NOT_FOUND, "用户不存在");
        }
        return ResponseResult.okResult();
    }

    @Override
    public ResponseResult<Void> resetPassword(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(AppHttpCodeEnum.BAD_REQUEST, "用户ID非法");
        }
        String hash = PASSWORD_ENCODER.encode("123456");
        int affected = adminUserMapper.updateUserPassword(id, hash);
        if (affected <= 0) {
            throw new BusinessException(AppHttpCodeEnum.NOT_FOUND, "用户不存在");
        }
        return ResponseResult.okResult();
    }

    private void validateSave(AdminUserDtos.SaveUserRequestDto req, boolean withPassword) {
        if (req == null) {
            throw new BusinessException(AppHttpCodeEnum.BAD_REQUEST, "请求体不能为空");
        }
        if (req.getUsername() == null || req.getUsername().isBlank()) {
            throw new BusinessException(AppHttpCodeEnum.BAD_REQUEST, "用户名不能为空");
        }
        if (req.getNickname() == null || req.getNickname().isBlank()) {
            throw new BusinessException(AppHttpCodeEnum.BAD_REQUEST, "昵称不能为空");
        }
        if (withPassword && (req.getPassword() == null || req.getPassword().isBlank())) {
            throw new BusinessException(AppHttpCodeEnum.BAD_REQUEST, "密码不能为空");
        }
    }

    private int normalizeStatus(Integer status) {
        return status == null ? 1 : (status == 0 ? 0 : 1);
    }

    // 前端传英文角色码，数据库存中文角色名
    private String normalizeRoleName(String role) {
        String key = role == null ? "" : role.trim().toUpperCase();
        return switch (key) {
            case "ADMIN", "管理员" -> "管理员";
            case "EDITOR", "编辑" -> "编辑";
            case "REVIEWER", "审核员" -> "审核员";
            case "AUTHOR", "博主", "文章博主" -> "博主";
            default -> role.trim();
        };
    }

    // 数据库读中文角色名，前端统一用英文角色码
    private String normalizeRoleCodeForUi(String roleName) {
        String key = roleName == null ? "" : roleName.trim();
        return switch (key) {
            case "管理员", "ADMIN" -> "ADMIN";
            case "编辑", "EDITOR" -> "EDITOR";
            case "审核员", "REVIEWER" -> "REVIEWER";
            case "博主", "文章博主", "AUTHOR" -> "AUTHOR";
            default -> key.toUpperCase();
        };
    }

    private AdminUserDtos.UserItemDto toDto(Map<String, Object> row) {
        AdminUserDtos.UserItemDto dto = new AdminUserDtos.UserItemDto();
        dto.setId(asLong(row.get("id")));
        dto.setUsername(asString(row.get("username")));
        dto.setNickname(asString(row.get("nickname")));
        dto.setSignature(asString(row.get("signature")));
        dto.setStatus(asInt(row.get("status")));
        dto.setLastLoginTime(asString(row.get("lastLoginTime")));
        dto.setCreateTime(asString(row.get("createTime")));
        List<String> roles = adminUserMapper.selectRoleCodesByUserId(dto.getId());
        dto.setRoles((roles == null || roles.isEmpty())
                ? List.of("AUTHOR")
                : roles.stream().map(this::normalizeRoleCodeForUi).distinct().toList());
        return dto;
    }

    private String asString(Object o) { return o == null ? "" : String.valueOf(o); }
    private Long asLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        return Long.parseLong(String.valueOf(o));
    }
    private Integer asInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        return Integer.parseInt(String.valueOf(o));
    }
}
