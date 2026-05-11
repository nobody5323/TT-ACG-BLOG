package nobody.domain.service;

import nobody.domain.entity.ResponseResult;
import nobody.dto.admin.AdminCommentDtos;

public interface AdminCommentService {
    ResponseResult<AdminCommentDtos.CommentListResponseDto> list(String keyword, Integer status, Long pageNum, Integer pageSize);
    ResponseResult<AdminCommentDtos.CommentItemDto> detail(Long id);
    ResponseResult<AdminCommentDtos.CommentItemDto> reply(Long id, AdminCommentDtos.ReplyRequestDto req);
    ResponseResult<Void> patchStatus(Long id, AdminCommentDtos.StatusPatchRequestDto req);
    ResponseResult<Void> delete(Long id);
    ResponseResult<Void> batchStatus(AdminCommentDtos.BatchStatusRequestDto req);
}
