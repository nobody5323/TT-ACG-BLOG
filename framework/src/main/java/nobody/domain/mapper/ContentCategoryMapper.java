package nobody.domain.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;


/**
 * 内容分类表(ContentCategory)表数据库访问层
 *
 * @author makejava
 * @since 2026-04-19 13:22:59
 */
@Mapper
public interface ContentCategoryMapper {

    List<Map<String, Object>> selectHotCategories(@Param("limit") int limit);
}
