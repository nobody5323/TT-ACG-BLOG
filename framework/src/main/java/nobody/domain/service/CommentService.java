package nobody.domain.service;

import nobody.domain.entity.ResponseResult;
import nobody.dto.comment.CommentDto;

public interface CommentService {
    ResponseResult<CommentDto.ArticleCommentsResponseDto> comments(String slug, Long cursor, Integer size);

    ResponseResult<CommentDto.CreateCommentResponseDto> createComments(String slug, String content);

    ResponseResult<CommentDto.CreateCommentResponseDto> replyComment(Long commentId, String content);

    ResponseResult<Void> deleteComment(Long commentId);
}
