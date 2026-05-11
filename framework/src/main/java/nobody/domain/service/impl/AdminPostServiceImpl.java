package nobody.domain.service.impl;

import nobody.Enum.AppHttpCodeEnum;
import nobody.domain.entity.ResponseResult;
import nobody.domain.mapper.AdminPostMapper;
import nobody.domain.service.AdminPostService;
import nobody.dto.admin.AdminPostDtos;
import nobody.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class AdminPostServiceImpl implements AdminPostService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private final AdminPostMapper adminPostMapper;

    public AdminPostServiceImpl(AdminPostMapper adminPostMapper) {
        this.adminPostMapper = adminPostMapper;
    }

    @Override
    public ResponseResult<AdminPostDtos.PostListResponseDto> list(String keyword, Integer publishStatus, Long pageNum, Integer pageSize) {
        long currentPage = pageNum == null || pageNum <= 0 ? 1L : pageNum;
        int size = normalizePageSize(pageSize);
        long offset = (currentPage - 1) * size;

        Long total = adminPostMapper.countPosts(keyword, publishStatus);
        List<Map<String, Object>> rows = adminPostMapper.selectPosts(keyword, publishStatus, offset, size);
        List<AdminPostDtos.PostItemDto> items = rows.stream().map(this::toPostItemDto).toList();

        return ResponseResult.okResult(new AdminPostDtos.PostListResponseDto(total == null ? 0L : total, items));
    }

    @Override
    public ResponseResult<AdminPostDtos.PostDetailDto> detail(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(AppHttpCodeEnum.BAD_REQUEST, "文章ID非法");
        }
        Map<String, Object> row = adminPostMapper.selectPostById(id);
        if (row == null || row.isEmpty()) {
            throw new BusinessException(AppHttpCodeEnum.NOT_FOUND, "文章不存在");
        }
        return ResponseResult.okResult(toPostDetailDto(row));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseResult<AdminPostDtos.PostDetailDto> createDraft(AdminPostDtos.SavePostRequestDto req) {
        validateSaveRequest(req);
        Long userId = currentUserId();

        int inserted = adminPostMapper.insertPost(userId, normalizeSaveRequest(req));
        if (inserted <= 0) {
            throw new BusinessException(AppHttpCodeEnum.SYSTEM_ERROR, "创建文章失败");
        }
        Long id = adminPostMapper.lastInsertId();
        return detail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseResult<AdminPostDtos.PostDetailDto> update(Long id, AdminPostDtos.SavePostRequestDto req) {
        if (id == null || id <= 0) {
            throw new BusinessException(AppHttpCodeEnum.BAD_REQUEST, "文章ID非法");
        }
        validateSaveRequest(req);
        int affected = adminPostMapper.updatePost(id, normalizeSaveRequest(req));
        if (affected <= 0) {
            throw new BusinessException(AppHttpCodeEnum.NOT_FOUND, "文章不存在或已删除");
        }
        return detail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseResult<Void> publish(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(AppHttpCodeEnum.BAD_REQUEST, "??ID??");
        }
        // ?? ??(0) ? ??(2) -> ???(1)
        int affected = adminPostMapper.updatePostStatus(id, 0, 1);
        if (affected <= 0) {
            affected = adminPostMapper.updatePostStatus(id, 2, 1);
        }
        if (affected <= 0) {
            throw new BusinessException(AppHttpCodeEnum.BIZ_ERROR, "???????????");
        }
        return ResponseResult.okResult();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseResult<Void> offline(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(AppHttpCodeEnum.BAD_REQUEST, "文章ID非法");
        }
        // 仅允许 已发布(1) -> 下线(2)
        int affected = adminPostMapper.updatePostStatus(id, 1, 2);
        if (affected <= 0) {
            throw new BusinessException(AppHttpCodeEnum.BIZ_ERROR, "仅已发布状态可下线");
        }
        return ResponseResult.okResult();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseResult<Void> delete(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(AppHttpCodeEnum.BAD_REQUEST, "文章ID非法");
        }
        int affected = adminPostMapper.logicDeletePost(id);
        if (affected <= 0) {
            throw new BusinessException(AppHttpCodeEnum.NOT_FOUND, "文章不存在或已删除");
        }
        return ResponseResult.okResult();
    }

    private AdminPostDtos.SavePostRequestDto normalizeSaveRequest(AdminPostDtos.SavePostRequestDto req) {
        if (req.getPostType() == null) {
            req.setPostType(1);
        }
        if (req.getReadingMinutes() == null) {
            req.setReadingMinutes(0);
        }
        if (req.getVisibility() == null) {
            req.setVisibility(1);
        }
        if (req.getIsTop() == null) {
            req.setIsTop(0);
        }
        if (req.getIsFeatured() == null) {
            req.setIsFeatured(0);
        }
        return req;
    }

    private void validateSaveRequest(AdminPostDtos.SavePostRequestDto req) {
        if (req == null) {
            throw new BusinessException(AppHttpCodeEnum.BAD_REQUEST, "请求体不能为空");
        }
        if (req.getTitle() == null || req.getTitle().isBlank()) {
            throw new BusinessException(AppHttpCodeEnum.BAD_REQUEST, "标题不能为空");
        }
        if (req.getTitle().length() > 200) {
            throw new BusinessException(AppHttpCodeEnum.BAD_REQUEST, "标题不能超过200字符");
        }
        if (req.getSummary() != null && req.getSummary().length() > 500) {
            throw new BusinessException(AppHttpCodeEnum.BAD_REQUEST, "摘要不能超过500字符");
        }
        if (req.getContent() == null || req.getContent().isBlank()) {
            throw new BusinessException(AppHttpCodeEnum.BAD_REQUEST, "正文不能为空");
        }
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private AdminPostDtos.PostItemDto toPostItemDto(Map<String, Object> row) {
        return new AdminPostDtos.PostItemDto(
                asLong(row.get("id")),
                asString(row.get("slug")),
                asString(row.get("title")),
                asString(row.get("summary")),
                asInt(row.get("publishStatus")),
                asInt(row.get("visibility")),
                asInt(row.get("isFeatured")),
                asInt(row.get("isTop")),
                asString(row.get("publishedTime")),
                asString(row.get("updateTime"))
        );
    }

    private AdminPostDtos.PostDetailDto toPostDetailDto(Map<String, Object> row) {
        AdminPostDtos.PostDetailDto dto = new AdminPostDtos.PostDetailDto();
        dto.setId(asLong(row.get("id")));
        dto.setSlug(asString(row.get("slug")));
        dto.setPostType(asInt(row.get("postType")));
        dto.setTitle(asString(row.get("title")));
        dto.setSummary(asString(row.get("summary")));
        dto.setReadingMinutes(asInt(row.get("readingMinutes")));
        dto.setCoverUrl(asString(row.get("coverUrl")));
        dto.setCoverTone(asString(row.get("coverTone")));
        dto.setContent(asString(row.get("content")));
        dto.setAuthorId(asLong(row.get("authorId")));
        dto.setBoardId(asLong(row.get("boardId")));
        dto.setCategoryId(asLong(row.get("categoryId")));
        dto.setColumnId(asLong(row.get("columnId")));
        dto.setPublishStatus(asInt(row.get("publishStatus")));
        dto.setVisibility(asInt(row.get("visibility")));
        dto.setIsTop(asInt(row.get("isTop")));
        dto.setIsFeatured(asInt(row.get("isFeatured")));
        dto.setRanking(asInt(row.get("ranking")));
        return dto;
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
