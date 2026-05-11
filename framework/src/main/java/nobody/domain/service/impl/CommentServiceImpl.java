package nobody.domain.service.impl;

import nobody.Enum.AppHttpCodeEnum;
import nobody.domain.entity.ResponseResult;
import nobody.domain.mapper.CommentsMapper;
import nobody.domain.service.CommentService;
import nobody.dto.comment.CommentDto;
import nobody.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CommentServiceImpl implements CommentService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_CONTENT_LENGTH = 500;

    private final CommentsMapper commentsMapper;

    public CommentServiceImpl(CommentsMapper commentsMapper) {
        this.commentsMapper = commentsMapper;
    }

    @Override
    public ResponseResult<CommentDto.ArticleCommentsResponseDto> comments(String slug, Long cursor, Integer size) {
        if (slug == null || slug.isBlank()) {
            throw new BusinessException(AppHttpCodeEnum.BAD_REQUEST, "slug 不能为空");
        }
        if (cursor != null && cursor <= 0) {
            throw new BusinessException(AppHttpCodeEnum.BAD_REQUEST, "cursor 必须大于 0");
        }

        int pageSize = normalizePageSize(size);
        List<Map<String, Object>> rows = commentsMapper.selectCommentsBySlug(slug, cursor, pageSize);
        List<CommentDto.CommentItemDto> items = rows.stream().map(this::toCommentItem).toList();
        buildCommentTree(items);

        Long nextCursor = null;
        if (!items.isEmpty() && items.size() == pageSize) {
            nextCursor = items.get(items.size() - 1).getId();
        }

        return ResponseResult.okResult(new CommentDto.ArticleCommentsResponseDto(items, nextCursor));
    }

    private void buildCommentTree(List<CommentDto.CommentItemDto> topItems) {
        if (topItems == null || topItems.isEmpty()) {
            return;
        }

        Map<Long, CommentDto.CommentItemDto> topById = new LinkedHashMap<>();
        for (CommentDto.CommentItemDto item : topItems) {
            item.setChildren(new ArrayList<>());
            if (item.getId() != null) {
                topById.put(item.getId(), item);
            }
        }
        if (topById.isEmpty()) {
            return;
        }

        List<Map<String, Object>> replyRows = commentsMapper.selectRepliesByRootIds(new ArrayList<>(topById.keySet()));
        if (replyRows == null || replyRows.isEmpty()) {
            return;
        }

        List<CommentDto.CommentItemDto> replies = replyRows.stream().map(this::toCommentItem).toList();
        Map<Long, CommentDto.CommentItemDto> allById = new LinkedHashMap<>(topById);
        for (CommentDto.CommentItemDto reply : replies) {
            reply.setChildren(new ArrayList<>());
            if (reply.getId() != null) {
                allById.put(reply.getId(), reply);
            }
        }

        for (CommentDto.CommentItemDto reply : replies) {
            Long parentId = reply.getParentId();
            CommentDto.CommentItemDto parent = parentId == null ? null : allById.get(parentId);
            if (parent != null) {
                parent.getChildren().add(reply);
                continue;
            }

            Long rootId = reply.getRootId();
            CommentDto.CommentItemDto root = rootId == null ? null : topById.get(rootId);
            if (root != null) {
                root.getChildren().add(reply);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseResult<CommentDto.CreateCommentResponseDto> createComments(String slug, String contentRaw) {
        Long userId = requireLoginUserId();
        String content = validateContent(contentRaw);

        Long postId = commentsMapper.selectPostIdBySlug(slug);
        if (postId == null) {
            throw new BusinessException(AppHttpCodeEnum.NOT_FOUND, "文章不存在");
        }

        Long parentId = 0L;
        Long rootId = 0L;
        commentsMapper.insertComment(postId, userId, parentId, rootId, content);
        Long commentId = commentsMapper.lastInsertId();
        commentsMapper.increasePostCommentCount(postId);

        Map<String, Object> row = commentsMapper.selectCommentById(commentId);
        if (row == null || row.isEmpty()) {
            throw new BusinessException(AppHttpCodeEnum.SYSTEM_ERROR, "评论创建失败");
        }

        return ResponseResult.okResult(toCreateResponse(toCommentItem(row)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseResult<CommentDto.CreateCommentResponseDto> replyComment(Long commentId, String contentRaw) {
        if (commentId == null || commentId <= 0) {
            throw new BusinessException(AppHttpCodeEnum.BAD_REQUEST, "评论ID非法");
        }
        Long userId = requireLoginUserId();
        String content = validateContent(contentRaw);

        Map<String, Object> parent = commentsMapper.selectCommentById(commentId);
        if (parent == null || parent.isEmpty()) {
            throw new BusinessException(AppHttpCodeEnum.NOT_FOUND, "父评论不存在");
        }

        Long postId = asLong(parent.get("postId"));
        Long parentId = asLong(parent.get("id"));
        Long currentRootId = asLong(parent.get("rootId"));
        Long rootId = (currentRootId == null || currentRootId == 0) ? parentId : currentRootId;

        commentsMapper.insertComment(postId, userId, parentId, rootId, content);
        Long newCommentId = commentsMapper.lastInsertId();
        commentsMapper.increasePostCommentCount(postId);

        Map<String, Object> row = commentsMapper.selectCommentById(newCommentId);
        if (row == null || row.isEmpty()) {
            throw new BusinessException(AppHttpCodeEnum.SYSTEM_ERROR, "回复创建失败");
        }

        return ResponseResult.okResult(toCreateResponse(toCommentItem(row)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseResult<Void> deleteComment(Long commentId) {
        if (commentId == null || commentId <= 0) {
            throw new BusinessException(AppHttpCodeEnum.BAD_REQUEST, "评论ID非法");
        }
        Long userId = requireLoginUserId();

        Map<String, Object> current = commentsMapper.selectCommentById(commentId);
        if (current == null || current.isEmpty()) {
            throw new BusinessException(AppHttpCodeEnum.NOT_FOUND, "评论不存在");
        }

        Long ownerId = asLong(current.get("userId"));
        if (ownerId == null || !ownerId.equals(userId)) {
            throw new BusinessException(AppHttpCodeEnum.FORBIDDEN, "仅可删除自己的评论");
        }

        int affected = commentsMapper.softDeleteCommentById(commentId);
        if (affected <= 0) {
            throw new BusinessException(AppHttpCodeEnum.SYSTEM_ERROR, "删除失败");
        }

        Long postId = asLong(current.get("postId"));
        if (postId != null) {
            commentsMapper.decreasePostCommentCount(postId);
        }

        return ResponseResult.okResult();
    }

    private Long requireLoginUserId() {
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

    private String validateContent(String contentRaw) {
        if (contentRaw == null || contentRaw.isBlank()) {
            throw new BusinessException(AppHttpCodeEnum.BAD_REQUEST, "评论内容不能为空");
        }
        String content = contentRaw.trim();
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException(AppHttpCodeEnum.BAD_REQUEST, "评论内容不能超过 500 字");
        }
        return content;
    }

    private int normalizePageSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private CommentDto.CreateCommentResponseDto toCreateResponse(CommentDto.CommentItemDto item) {
        return new CommentDto.CreateCommentResponseDto(
                item.getId(),
                item.getPostId(),
                item.getUserId(),
                item.getParentId(),
                item.getRootId(),
                item.getContent(),
                item.getLikeCount(),
                item.getCreateTime(),
                item.getNickname()
        );
    }

    private CommentDto.CommentItemDto toCommentItem(Map<String, Object> r) {
        return new CommentDto.CommentItemDto(
                asLong(r.get("id")),
                asLong(r.get("postId")),
                asLong(r.get("userId")),
                asLong(r.get("parentId")),
                asLong(r.get("rootId")),
                asString(r.get("content")),
                asInt(r.get("likeCount")),
                asString(r.get("createTime")),
                asString(r.get("nickname")),
                new ArrayList<>()
        );
    }

    private String asString(Object o) {
        return o == null ? "" : String.valueOf(o);
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

    private Integer asInt(Object o) {
        if (o == null) {
            return 0;
        }
        if (o instanceof Number n) {
            return n.intValue();
        }
        return Integer.parseInt(String.valueOf(o));
    }
}
