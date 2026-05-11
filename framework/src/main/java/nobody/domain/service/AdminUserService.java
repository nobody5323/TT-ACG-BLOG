package nobody.domain.service;

import nobody.domain.entity.ResponseResult;
import nobody.dto.admin.AdminUserDtos;

public interface AdminUserService {
    ResponseResult<AdminUserDtos.UserListResponseDto> list(String keyword, Integer status, Long pageNum, Integer pageSize);
    ResponseResult<AdminUserDtos.UserItemDto> detail(Long id);
    ResponseResult<AdminUserDtos.UserItemDto> create(AdminUserDtos.SaveUserRequestDto req);
    ResponseResult<AdminUserDtos.UserItemDto> update(Long id, AdminUserDtos.SaveUserRequestDto req);
    ResponseResult<Void> patchRoles(Long id, AdminUserDtos.PatchRolesRequestDto req);
    ResponseResult<Void> patchStatus(Long id, AdminUserDtos.PatchStatusRequestDto req);
    ResponseResult<Void> resetPassword(Long id);
}
