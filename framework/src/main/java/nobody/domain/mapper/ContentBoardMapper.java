package nobody.domain.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;


/**
 * 内容板块表(ContentBoard)表数据库访问层
 *
 * @author makejava
 * @since 2026-04-19 13:22:59
 */
@Mapper
public interface ContentBoardMapper {

    List<Map<String, Object>> selectBoards();
}
