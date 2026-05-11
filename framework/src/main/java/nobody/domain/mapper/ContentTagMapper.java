package nobody.domain.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;


/**
 * 标签表(ContentTag)表数据库访问层
 *
 * @author makejava
 * @since 2026-04-19 13:22:59
 */
@Mapper
public interface ContentTagMapper {

    List<Map<String, Object>> selectHotTags(@Param("limit") int limit);
}
