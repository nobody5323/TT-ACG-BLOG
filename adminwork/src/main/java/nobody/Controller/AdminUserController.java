package nobody.Controller;

import nobody.domain.entity.ResponseResult;
import nobody.domain.service.AdminUserService;
import nobody.dto.admin.AdminUserDtos;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ResponseResult<AdminUserDtos.UserListResponseDto> list(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "pageNum", required = false) Long pageNum,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return adminUserService.list(keyword, status, pageNum, pageSize);
    }

    @GetMapping("/{id}")
    public ResponseResult<AdminUserDtos.UserItemDto> detail(@PathVariable("id") Long id) {
        return adminUserService.detail(id);
    }

    @PostMapping
    public ResponseResult<AdminUserDtos.UserItemDto> create(@RequestBody AdminUserDtos.SaveUserRequestDto req) {
        return adminUserService.create(req);
    }

    @PutMapping("/{id}")
    public ResponseResult<AdminUserDtos.UserItemDto> update(
            @PathVariable("id") Long id,
            @RequestBody AdminUserDtos.SaveUserRequestDto req) {
        return adminUserService.update(id, req);
    }

    @PatchMapping("/{id}/roles")
    public ResponseResult<Void> patchRoles(
            @PathVariable("id") Long id,
            @RequestBody AdminUserDtos.PatchRolesRequestDto req) {
        return adminUserService.patchRoles(id, req);
    }

    @PatchMapping("/{id}/status")
    public ResponseResult<Void> patchStatus(
            @PathVariable("id") Long id,
            @RequestBody AdminUserDtos.PatchStatusRequestDto req) {
        return adminUserService.patchStatus(id, req);
    }

    @PostMapping("/{id}/reset-password")
    public ResponseResult<Void> resetPassword(@PathVariable("id") Long id) {
        return adminUserService.resetPassword(id);
    }
}
