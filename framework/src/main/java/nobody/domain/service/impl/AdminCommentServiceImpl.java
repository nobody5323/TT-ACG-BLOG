package nobody.domain.service.impl;

import nobody.Enum.AppHttpCodeEnum;
import nobody.domain.entity.ResponseResult;
import nobody.domain.mapper.AdminCommentMapper;
import nobody.domain.service.AdminCommentService;
import nobody.dto.admin.AdminCommentDtos;
import nobody.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AdminCommentServiceImpl implements AdminCommentService {

    private final AdminCommentMapper adminCommentMapper;

    public AdminCommentServiceImpl(AdminCommentMapper adminCommentMapper) {
        this.adminCommentMapper = adminCommentMapper;
    }

    @Override
    public ResponseResult<AdminCommentDtos.CommentListResponseDto> list(String keyword, Integer status, Long pageNum, Integer pageSize) {
        long current = pageNum == null || pageNum <= 0 ? 1 : pageNum;
        int size = pageSize == null || pageSize <= 0 ? 10 : Math.min(pageSize, 100);
        long offset = (current - 1) * size;

        Long total = adminCommentMapper.countComments(keyword, status);
        List<Map<String, Object>> rows = adminCommentMapper.selectComments(keyword, status, offset, size);
        List<AdminCommentDtos.CommentItemDto> items = rows.stream().map(this::toDto).toList();

        AdminCommentDtos.CommentListResponseDto dto = new AdminCommentDtos.CommentListResponseDto();
        dto.setTotal(total == null ? 0L : total);
        dto.setItems(items);
        return ResponseResult.okResult(dto);
    }

    @Override
    public ResponseResult<AdminCommentDtos.CommentItemDto> detail(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(AppHttpCodeEnum.BAD_REQUEST, "评论ID非法");
        }
        Map<String, Object> row = adminCommentMapper.selectCommentById(id);
        if (row == null || row.isEmpty()) {
            throw new BusinessException(AppHttpCodeEnum.NOT_FOUND, "评论不存在");
        }
        AdminCommentDtos.CommentItemDto dto = toDto(row);
        Long rootId = dto.getRootId() != null && dto.getRootId() != 0 ? dto.getRootId() : dto.getId();
        List<Map<String, Object>> replyRows = adminCommentMapper.selectRepliesByRootId(rootId);
        List<AdminCommentDtos.CommentItemDto> replies = replyRows.stream().map(this::toDto).toList();
        dto.setReplies(replies);
        return ResponseResult.okResult(dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseResult<AdminCommentDtos.CommentItemDto> reply(Long id, AdminCommentDtos.ReplyRequestDto req) {
        if (id == null || id <= 0) {
            throw new BusinessException(AppHttpCodeEnum.BAD_REQUEST, "评论ID非法");
        }
        if (req == null || req.getContent() == null || req.getContent().isBlank()) {
            throw new BusinessException(AppHttpCodeEnum.BAD_REQUEST, "回复内容不能为空");
        }
        Map<String, Object> parent = adminCommentMapper.selectCommentById(id);
        if (parent == null || parent.isEmpty()) {
            throw new BusinessException(AppHttpCodeEnum.NOT_FOUND, "父评论不存在");
        }
        Long userId = currentUserId();
        Long postId = asLong(parent.get("postId"));
        Long parentId = asLong(parent.get("id"));
        Long parentRootId = asLong(parent.get("rootId"));
        Long rootId = (parentRootId == null || parentRootId == 0) ? parentId : parentRootId;

        adminCommentMapper.insertReply(postId, userId, parentId, rootId, req.getContent().trim());
        Long newId = adminCommentMapper.lastInsertId();
        adminCommentMapper.increasePostCommentCount(postId);

        Map<String, Object> row = adminCommentMapper.selectCommentById(newId);
        if (row == null || row.isEmpty()) {
            throw new BusinessException(AppHttpCodeEnum.SYSTEM_ERROR, "回复失败");
        }
        return ResponseResult.okResult(toDto(row));
    }

    @Override
    public ResponseResult<Void> patchStatus(Long id, AdminCommentDtos.StatusPatchRequestDto req) {
        if (id == null || id <= 0) {
            throw new BusinessException(AppHttpCodeEnum.BAD_REQUEST, "评论ID非法");
        }
        if (req == null || (req.getStatus() != 0 && req.getStatus() != 1)) {
            throw new BusinessException(AppHttpCodeEnum.BAD_REQUEST, "状态只允许 0/1");
        }
        int affected = adminCommentMapper.updateCommentStatus(id, req.getStatus());
        if (affected <= 0) {
            throw new BusinessException(AppHttpCodeEnum.NOT_FOUND, "评论不存在");
        }
        return ResponseResult.okResult();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseResult<Void> delete(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(AppHttpCodeEnum.BAD_REQUEST, "评论ID非法");
        }
        Map<String, Object> row = adminCommentMapper.selectCommentById(id);
        if (row == null || row.isEmpty()) {
            throw new BusinessException(AppHttpCodeEnum.NOT_FOUND, "评论不存在");
        }
        int affected = adminCommentMapper.softDeleteComment(id);
        if (affected <= 0) {
            throw new BusinessException(AppHttpCodeEnum.SYSTEM_ERROR, "删除失败");
        }
        Long postId = asLong(row.get("postId"));
        if (postId != null) {
            adminCommentMapper.decreasePostCommentCount(postId);
        }
        return ResponseResult.okResult();
    }

    @Override
    public ResponseResult<Void> batchStatus(AdminCommentDtos.BatchStatusRequestDto req) {
        if (req == null || req.getIds() == null || req.getIds().isEmpty()) {
            throw new BusinessException(AppHttpCodeEnum.BAD_REQUEST, "评论ID列表不能为空");
        }
        if (req.getStatus() == null || (req.getStatus() != 0 && req.getStatus() != 1)) {
            throw new BusinessException(AppHttpCodeEnum.BAD_REQUEST, "状态只允许 0/1");
        }
        int affected = adminCommentMapper.batchUpdateStatus(req.getIds(), req.getStatus());
        if (affected <= 0) {
            throw new BusinessException(AppHttpCodeEnum.NOT_FOUND, "未匹配到可更新评论");
        }
        return ResponseResult.okResult();
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException(AppHttpCodeEnum.UNAUTHORIZED);
        }
        try {
            return Long.valueOf(String.valueOf(authentication.getPrincipal()));
        } catch (Exception e) {
            throw new BusinessException(AppHttpCodeEnum.UNAUTHORIZED, "登录状态无效");
        }
    }

    private AdminCommentDtos.CommentItemDto toDto(Map<String, Object> row) {
        AdminCommentDtos.CommentItemDto dto = new AdminCommentDtos.CommentItemDto();
        dto.setId(asLong(row.get("id")));
        dto.setPostId(asLong(row.get("postId")));
        dto.setUserId(asLong(row.get("userId")));
        dto.setParentId(asLong(row.get("parentId")));
        dto.setRootId(asLong(row.get("rootId")));
        dto.setContent(asString(row.get("content")));
        dto.setStatus(asInt(row.get("status")));
        dto.setDeleted(asInt(row.get("deleted")));
        dto.setLikeCount(asInt(row.get("likeCount")));
        dto.setCreateTime(asString(row.get("createTime")));
        dto.setNickname(asString(row.get("nickname")));
        dto.setArticleTitle(asString(row.get("articleTitle")));
        dto.setReplies(new ArrayList<>());
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
