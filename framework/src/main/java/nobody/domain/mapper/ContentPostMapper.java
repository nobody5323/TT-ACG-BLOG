package nobody.domain.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;


/**
 * 内容主表(ContentPost)表数据库访问层
 *
 * @author makejava
 * @since 2026-04-19 13:22:59
 */
@Mapper
public interface ContentPostMapper {

    List<Map<String, Object>> selectFeaturedArticles(@Param("limit") int limit);

    List<Map<String, Object>> selectLatestArticles(@Param("limit") int limit);

    List<Map<String, Object>> selectRankingArticles(@Param("limit") int limit);

    List<Map<String, Object>> selectHeroFeaturedArticle();

    List<Map<String, Object>> selectArticles(@Param("category") String category,
                                             @Param("tag") String tag,
                                             @Param("keyword") String keyword,
                                             @Param("popularSort") boolean popularSort);

    List<Map<String, Object>> selectArticleBySlug(@Param("slug") String slug);

    List<Map<String, Object>> selectRelatedArticles(@Param("slug") String slug,
                                                    @Param("limit") int limit);

    List<Map<String, Object>> searchArticles(@Param("keyword") String keyword);

    int incrementViewCount(@Param("slug") String slug);

    int incrementLikeCount(@Param("slug") String slug, @Param("delta") int delta);

    Map<String, Object> selectCountsBySlug(@Param("slug") String slug);

    Long selectPostIdBySlug(@Param("slug") String slug);

    int insertPostLike(@Param("postId") Long postId,
                       @Param("userId") Long userId,
                       @Param("clientId") String clientId);

}
