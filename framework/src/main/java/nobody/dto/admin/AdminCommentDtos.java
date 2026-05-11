package nobody.dto.admin;

import lombok.Data;

import java.util.List;

public class AdminCommentDtos {

    @Data
    public static class CommentItemDto {
        private Long id;
        private Long postId;
        private Long userId;
        private Long parentId;
        private Long rootId;
        private String content;
        private Integer status;
        private Integer deleted;
        private Integer likeCount;
        private String createTime;
        private String nickname;
        private String articleTitle;
        private List<CommentItemDto> replies;
    }

    @Data
    public static class CommentListResponseDto {
        private Long total;
        private List<CommentItemDto> items;
    }

    @Data
    public static class ReplyRequestDto {
        private String content;
    }

    @Data
    public static class StatusPatchRequestDto {
        private Integer status;
    }

    @Data
    public static class BatchStatusRequestDto {
        private List<Long> ids;
        private Integer status;
    }
}
