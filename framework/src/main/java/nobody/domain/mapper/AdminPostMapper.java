package nobody.domain.mapper;

import nobody.dto.admin.AdminPostDtos;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface AdminPostMapper {

    Long countPosts(@Param("keyword") String keyword, @Param("publishStatus") Integer publishStatus);

    List<Map<String, Object>> selectPosts(@Param("keyword") String keyword,
                                          @Param("publishStatus") Integer publishStatus,
                                          @Param("offset") Long offset,
                                          @Param("size") Integer size);

    Map<String, Object> selectPostById(@Param("id") Long id);

    int insertPost(@Param("authorId") Long authorId, @Param("req") AdminPostDtos.SavePostRequestDto req);

    Long lastInsertId();

    int updatePost(@Param("id") Long id, @Param("req") AdminPostDtos.SavePostRequestDto req);

    int updatePostStatus(@Param("id") Long id, @Param("fromStatus") Integer fromStatus, @Param("toStatus") Integer toStatus);

    int logicDeletePost(@Param("id") Long id);
}
