package nobody.domain.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserProfileMapper {

    Map<String, Object> selectProfileBaseByUserId(@Param("userId") Long userId);

    Map<String, Object> selectProfileStatsByUserId(@Param("userId") Long userId);

    List<Map<String, Object>> selectFavoriteArticlesByUserId(@Param("userId") Long userId,
                                                             @Param("limit") Integer limit);

    List<Map<String, Object>> selectHistoryArticlesByUserId(@Param("userId") Long userId,
                                                            @Param("limit") Integer limit);
}
