package nobody.domain.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface CommentsMapper {

    List<Map<String, Object>> selectCommentsBySlug(@Param("slug") String slug,
                                                   @Param("cursor") Long cursor,
                                                   @Param("size") Integer size);

    List<Map<String, Object>> selectRepliesByRootIds(@Param("rootIds") List<Long> rootIds);

    Long selectPostIdBySlug(@Param("slug") String slug);

    int insertComment(@Param("postId") Long postId,
                      @Param("userId") Long userId,
                      @Param("parentId") Long parentId,
                      @Param("rootId") Long rootId,
                      @Param("content") String content);

    Long lastInsertId();

    int increasePostCommentCount(@Param("postId") Long postId);

    int decreasePostCommentCount(@Param("postId") Long postId);

    Map<String, Object> selectCommentById(@Param("id") Long id);

    int softDeleteCommentById(@Param("id") Long id);
}
