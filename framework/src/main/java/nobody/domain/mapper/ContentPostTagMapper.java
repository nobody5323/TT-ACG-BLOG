package nobody.domain.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;


/**
 * 内容标签关联表(ContentPostTag)表数据库访问层
 *
 * @author makejava
 * @since 2026-04-19 12:56:15
 */
@Mapper
public interface ContentPostTagMapper {

    List<Map<String, Object>> selectTagNamesByPostIds(@Param("postIds") List<Long> postIds);
}
