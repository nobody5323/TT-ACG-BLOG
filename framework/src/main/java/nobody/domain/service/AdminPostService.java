package nobody.domain.service;

import nobody.domain.entity.ResponseResult;
import nobody.dto.admin.AdminPostDtos;

public interface AdminPostService {

    ResponseResult<AdminPostDtos.PostListResponseDto> list(String keyword, Integer publishStatus, Long pageNum, Integer pageSize);

    ResponseResult<AdminPostDtos.PostDetailDto> detail(Long id);

    ResponseResult<AdminPostDtos.PostDetailDto> createDraft(AdminPostDtos.SavePostRequestDto req);

    ResponseResult<AdminPostDtos.PostDetailDto> update(Long id, AdminPostDtos.SavePostRequestDto req);

    ResponseResult<Void> publish(Long id);

    ResponseResult<Void> offline(Long id);

    ResponseResult<Void> delete(Long id);
}
