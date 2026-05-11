package nobody.domain.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface AdminCommentMapper {

    Long countComments(@Param("keyword") String keyword, @Param("status") Integer status);

    List<Map<String, Object>> selectComments(@Param("keyword") String keyword,
                                             @Param("status") Integer status,
                                             @Param("offset") Long offset,
                                             @Param("size") Integer size);

    Map<String, Object> selectCommentById(@Param("id") Long id);

    List<Map<String, Object>> selectRepliesByRootId(@Param("rootId") Long rootId);

    int insertReply(@Param("postId") Long postId,
                    @Param("userId") Long userId,
                    @Param("parentId") Long parentId,
                    @Param("rootId") Long rootId,
                    @Param("content") String content);

    Long lastInsertId();

    int updateCommentStatus(@Param("id") Long id, @Param("status") Integer status);

    int softDeleteComment(@Param("id") Long id);

    int increasePostCommentCount(@Param("postId") Long postId);

    int decreasePostCommentCount(@Param("postId") Long postId);

    int batchUpdateStatus(@Param("ids") List<Long> ids, @Param("status") Integer status);
}
