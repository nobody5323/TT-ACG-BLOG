package nobody.dto.comment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class CommentDto {
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ArticleCommentsResponseDto {
        private List<CommentItemDto> items;
        private Long nextCursor;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommentItemDto {
        private Long id;
        private Long postId;
        private Long userId;
        private Long parentId;
        private Long rootId;
        private String content;
        private Integer likeCount;
        private String createTime;
        private String nickname;
        private List<CommentItemDto> children;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateCommentRequestDto {
        private String content;
        // 先做一级评论可不需要 parentId/rootId；
        // 后续做回复时再加：
        // private Long parentId;
        // private Long rootId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateCommentResponseDto {
        private Long id;
        private Long postId;
        private Long userId;
        private Long parentId;
        private Long rootId;
        private String content;
        private Integer likeCount;
        private String createTime;
        private String nickname;
    }
}
