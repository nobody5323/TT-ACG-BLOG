package nobody.domain.service.impl;

import nobody.Enum.AppHttpCodeEnum;
import nobody.domain.entity.ResponseResult;
import nobody.domain.mapper.ContentBoardMapper;
import nobody.domain.mapper.ContentCategoryMapper;
import nobody.domain.mapper.ContentPostMapper;
import nobody.domain.mapper.ContentPostTagMapper;
import nobody.domain.mapper.ContentTagMapper;
import nobody.domain.service.ContentService;
import nobody.dto.content.ArticlesResponseSlugDto;
import nobody.dto.content.ArticlesResponseDto;
import nobody.dto.content.HomeContentDtos;
import nobody.dto.content.HomeContentDtos.ArticleDto;
import nobody.dto.content.HomeContentDtos.BoardDto;
import nobody.dto.content.HomeContentDtos.CategoryDto;
import nobody.dto.content.HomeContentDtos.HeroDto;
import nobody.dto.content.HomeContentDtos.HeroMetricDto;
import nobody.dto.content.HomeContentDtos.HomeResponseDto;
import nobody.dto.content.HomeContentDtos.TagDto;
import nobody.exception.BusinessException;
import nobody.utils.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ContentServiceImpl implements ContentService {

    private static final int FEATURED_LIMIT = 3;
    private static final int LATEST_LIMIT = 5;
    private static final int RANKING_LIMIT = 5;
    private static final int HOT_CATEGORY_LIMIT = 8;
    private static final int HOT_TAG_LIMIT = 12;

    private final ContentPostMapper contentPostMapper;
    private final ContentCategoryMapper contentCategoryMapper;
    private final ContentTagMapper contentTagMapper;
    private final ContentPostTagMapper contentPostTagMapper;
    private final ContentBoardMapper contentBoardMapper;

    public ContentServiceImpl(ContentPostMapper contentPostMapper,
                              ContentCategoryMapper contentCategoryMapper,
                              ContentTagMapper contentTagMapper,
                              ContentPostTagMapper contentPostTagMapper,
                              ContentBoardMapper contentBoardMapper) {
        this.contentPostMapper = contentPostMapper;
        this.contentCategoryMapper = contentCategoryMapper;
        this.contentTagMapper = contentTagMapper;
        this.contentPostTagMapper = contentPostTagMapper;
        this.contentBoardMapper = contentBoardMapper;
    }

    @Override
    public ResponseResult<HomeResponseDto> home() {
        List<Map<String, Object>> featuredRows = safeRows(() -> contentPostMapper.selectFeaturedArticles(FEATURED_LIMIT));
        List<Map<String, Object>> latestRows = safeRows(() -> contentPostMapper.selectLatestArticles(LATEST_LIMIT));
        List<Map<String, Object>> rankingRows = safeRows(() -> contentPostMapper.selectRankingArticles(RANKING_LIMIT));
        List<Map<String, Object>> heroRows = safeRows(contentPostMapper::selectHeroFeaturedArticle);

        Map<Long, List<String>> tagsByPostId = loadTagsByPostId(mergeArticleIds(featuredRows, latestRows, rankingRows, heroRows));

        List<ArticleDto> featuredArticles = mapArticles(featuredRows, tagsByPostId);
        List<ArticleDto> latestArticles = mapArticles(latestRows, tagsByPostId);
        List<ArticleDto> rankings = mapArticles(rankingRows, tagsByPostId);
        ArticleDto heroFeatured = mapArticles(heroRows, tagsByPostId).stream().findFirst().orElse(emptyArticle());

        List<CategoryDto> hotCategories = new ArrayList<>();
        for (Map<String, Object> row : safeRows(() -> contentCategoryMapper.selectHotCategories(HOT_CATEGORY_LIMIT))) {
            hotCategories.add(toCategoryDto(row));
        }

        List<TagDto> hotTags = new ArrayList<>();
        for (Map<String, Object> row : safeRows(() -> contentTagMapper.selectHotTags(HOT_TAG_LIMIT))) {
            hotTags.add(toTagDto(row));
        }

        List<BoardDto> boards = new ArrayList<>();
        for (Map<String, Object> row : safeRows(contentBoardMapper::selectBoards)) {
            boards.add(toBoardDto(row));
        }

        HeroDto hero = new HeroDto(
                "Home",
                "ACG Content Home",
                "Content feed based on current database records",
                heroFeatured,
                List.of(
                        new HeroMetricDto("Featured", String.valueOf(featuredArticles.size())),
                        new HeroMetricDto("Latest", String.valueOf(latestArticles.size())),
                        new HeroMetricDto("Rankings", String.valueOf(rankings.size()))
                )
        );

            HomeResponseDto response = new HomeResponseDto(
                    hero,
                    featuredArticles,
                    hotCategories,
                    hotTags,
                    latestArticles,
                    defaultColumns(),
                    rankings,
                    List.of(),
                    boards,
                    defaultAnnouncements(),
                    defaultEditorialSignals(),
                    defaultWeeklySchedule(),
                    defaultStationProtocols()
            );
            return ResponseResult.okResult(response);
    }

    @Override
    public ResponseResult<ArticlesResponseDto> articles(String category, String tag, String keyword, String sort) {
        try {
            // Default sort when client omits sort parameter.
            String finalSort = (sort == null || sort.isBlank()) ? "latest" : sort;
            boolean popularSort = "popular".equalsIgnoreCase(finalSort);

            List<Map<String, Object>> articleRows = defaultList(
                    contentPostMapper.selectArticles(category, tag, keyword, popularSort)
            );

            Set<Long> postIds = mergeArticleIds(articleRows);
            Map<Long, List<String>> tagsByPostId = loadTagsByPostId(postIds);
            List<ArticleDto> items = mapArticles(articleRows, tagsByPostId);

            List<CategoryDto> categories = new ArrayList<>();
            for (Map<String, Object> row : defaultList(contentCategoryMapper.selectHotCategories(200))) {
                categories.add(toCategoryDto(row));
            }

            List<TagDto> tags = new ArrayList<>();
            for (Map<String, Object> row : defaultList(contentTagMapper.selectHotTags(200))) {
                tags.add(toTagDto(row));
            }

            ArticlesResponseDto.FiltersDto filters = new ArticlesResponseDto.FiltersDto(categories, tags);
            ArticlesResponseDto data = new ArticlesResponseDto(items, filters);
            return ResponseResult.okResult(data);
        } catch (Exception e) {
            ArticlesResponseDto empty = new ArticlesResponseDto(
                    List.of(),
                    new ArticlesResponseDto.FiltersDto(List.of(), List.of())
            );
            return ResponseResult.okResult(empty);
        }
    }

    @Override
    public ResponseResult<ArticlesResponseSlugDto> slug(String slug) {
        try {
            if (slug == null || slug.isBlank()) {
                return ResponseResult.okResult(new ArticlesResponseSlugDto(null, List.of()));
            }

            // 1) 查详情
            List<Map<String, Object>> articleRows = defaultList(contentPostMapper.selectArticleBySlug(slug));
            if (articleRows.isEmpty()) {
                return ResponseResult.okResult(new ArticlesResponseSlugDto(null, List.of()));
            }

            // 2) 查相关推荐
            List<Map<String, Object>> relatedRows = defaultList(contentPostMapper.selectRelatedArticles(slug, 3));

            // 3) 合并 ID 后批量查 tags
            Set<Long> ids = mergeArticleIds(articleRows, relatedRows);
            Map<Long, List<String>> tagsByPostId = loadTagsByPostId(ids);

            // 4) 组装 DTO
            HomeContentDtos.ArticleDto article = mapArticles(articleRows, tagsByPostId).get(0);
            List<HomeContentDtos.ArticleDto> related = mapArticles(relatedRows, tagsByPostId);

            return ResponseResult.okResult(new ArticlesResponseSlugDto(article, related));
        } catch (Exception e) {
            return ResponseResult.okResult(new ArticlesResponseSlugDto(null, List.of()));
        }
    }

    @Override
    public ResponseResult<Map<String, Object>> incrementView(String slug) {
        contentPostMapper.incrementViewCount(slug);
        return ResponseResult.okResult(countsBySlug(slug));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseResult<Map<String, Object>> toggleLike(String slug, String clientKey) {
        String normalizedClientKey = clientKey == null ? "" : clientKey.trim();
        if (normalizedClientKey.isBlank()) {
            throw new BusinessException(AppHttpCodeEnum.BAD_REQUEST, "缺少点赞访客标识");
        }

        Long userId = SecurityUtils.getUserId();

        Long postId = contentPostMapper.selectPostIdBySlug(slug);
        if (postId == null) {
            throw new BusinessException(AppHttpCodeEnum.NOT_FOUND, "文章不存在");
        }

        int inserted = contentPostMapper.insertPostLike(postId, userId, normalizedClientKey);
        if (inserted > 0) {
            contentPostMapper.incrementLikeCount(slug, 1);
        }

        Map<String, Object> counts = countsBySlug(slug);
        counts.put("liked", true);
        counts.put("likeChanged", inserted > 0);
        return ResponseResult.okResult(counts);
    }

    @Override
    public ResponseResult<List<HomeContentDtos.ArticleDto>> search(String keyword) {
        try {
            // 和前端 fallback 行为一致：关键词为空时返回空数组
            if (keyword == null || keyword.isBlank()) {
                return ResponseResult.okResult(List.of());
            }

            List<Map<String, Object>> rows = defaultList(contentPostMapper.searchArticles(keyword));

            Set<Long> postIds = mergeArticleIds(rows);
            Map<Long, List<String>> tagsByPostId = loadTagsByPostId(postIds);

            List<HomeContentDtos.ArticleDto> items = mapArticles(rows, tagsByPostId);
            return ResponseResult.okResult(items);
        } catch (Exception e) {
            return ResponseResult.okResult(List.of());
        }
    }



    private HomeResponseDto emptyResponse() {
        HeroDto hero = new HeroDto("", "", "", emptyArticle(), List.of());
        return new HomeResponseDto(
                hero,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private ArticleDto emptyArticle() {
        return new ArticleDto(
                null,
                "",
                "",
                "",
                "",
                "",
                List.of(),
                "",
                "",
                0,
                "",
                "",
                false,
                0,
                0,
                0,
                0,
                "",
                List.of()
        );
    }

    private <T> List<T> defaultList(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }

    @FunctionalInterface
    private interface ListSupplier<T> {
        List<T> get();
    }

    private <T> List<T> safeRows(ListSupplier<T> supplier) {
        try {
            return defaultList(supplier.get());
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private Map<String, Object> countsBySlug(String slug) {
        Map<String, Object> counts = contentPostMapper.selectCountsBySlug(slug);
        if (counts == null) {
            counts = new HashMap<>();
            counts.put("views", 0);
            counts.put("likes", 0);
            counts.put("favorites", 0);
        }
        return counts;
    }

    @SafeVarargs
    private Set<Long> mergeArticleIds(List<Map<String, Object>>... rowGroups) {
        Set<Long> ids = new LinkedHashSet<>();
        for (List<Map<String, Object>> group : rowGroups) {
            for (Map<String, Object> row : group) {
                Long id = asLong(row.get("id"));
                if (id != null) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }

    private Map<Long, List<String>> loadTagsByPostId(Set<Long> postIds) {
        if (postIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = defaultList(contentPostTagMapper.selectTagNamesByPostIds(new ArrayList<>(postIds)));
        Map<Long, List<String>> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long postId = asLong(row.get("postId"));
            String tagName = asString(row.get("tagName"));
            if (postId == null || tagName.isBlank()) {
                continue;
            }
            result.computeIfAbsent(postId, ignored -> new ArrayList<>()).add(tagName);
        }
        return result;
    }

    private List<ArticleDto> mapArticles(List<Map<String, Object>> rows, Map<Long, List<String>> tagsByPostId) {
        List<ArticleDto> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            result.add(toArticleDto(row, tagsByPostId));
        }
        return result;
    }

    private ArticleDto toArticleDto(Map<String, Object> row, Map<Long, List<String>> tagsByPostId) {
        Long id = asLong(row.get("id"));
        List<String> tags = id == null ? List.of() : tagsByPostId.getOrDefault(id, List.of());
        return new ArticleDto(
                id,
                asString(row.get("slug")),
                asString(row.get("title")),
                asString(row.get("summary")),
                asString(row.get("category")),
                asString(row.get("categorySlug")),
                tags,
                asString(row.get("author")),
                asString(row.get("publishedAt")),
                asInteger(row.get("readingMinutes")),
                asPublicPath(row.get("coverUrl")),
                asString(row.get("coverTone")),
                asInteger(row.get("featured")) == 1,
                asInteger(row.get("ranking")),
                asInteger(row.get("views")),
                asInteger(row.get("likes")),
                asInteger(row.get("favorites")),
                asString(row.get("column")),
                toArticleSections(row.get("content"))
        );
    }

    private List<HomeContentDtos.ArticleSectionDto> toArticleSections(Object value) {
        String content = asString(value).replace("\r\n", "\n").trim();
        if (content.isBlank()) {
            return List.of();
        }

        content = content.replaceAll("(?m)^#tags\\s+.*$", "").trim();
        List<HomeContentDtos.ArticleSectionDto> sections = new ArrayList<>();
        String currentHeading = "正文";
        List<String> paragraphs = new ArrayList<>();

        for (String block : content.split("\\n\\s*\\n")) {
            String cleaned = block.trim();
            if (cleaned.isBlank()) {
                continue;
            }

            String[] lines = cleaned.split("\\n");
            int paragraphStart = 0;
            String firstLine = lines[0].trim();
            if (firstLine.matches("^#{1,6}\\s+.+")) {
                if (!paragraphs.isEmpty()) {
                    sections.add(new HomeContentDtos.ArticleSectionDto(currentHeading, paragraphs));
                    paragraphs = new ArrayList<>();
                }
                currentHeading = firstLine.replaceFirst("^#{1,6}\\s+", "").trim();
                paragraphStart = 1;
            }

            StringBuilder paragraph = new StringBuilder();
            for (int i = paragraphStart; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.isBlank()) {
                    continue;
                }
                if (!paragraph.isEmpty()) {
                    paragraph.append('\n');
                }
                paragraph.append(line);
            }
            if (!paragraph.isEmpty()) {
                paragraphs.add(paragraph.toString());
            }
        }

        if (!paragraphs.isEmpty() || sections.isEmpty()) {
            sections.add(new HomeContentDtos.ArticleSectionDto(currentHeading, paragraphs));
        }
        return sections;
    }

    private CategoryDto toCategoryDto(Map<String, Object> row) {
        return new CategoryDto(
                asLong(row.get("id")),
                asString(row.get("name")),
                asString(row.get("slug")),
                asString(row.get("description")),
                asString(row.get("accent"))
        );
    }

    private TagDto toTagDto(Map<String, Object> row) {
        return new TagDto(
                asLong(row.get("id")),
                asString(row.get("name")),
                asInteger(row.get("heat"))
        );
    }

    private BoardDto toBoardDto(Map<String, Object> row) {
        Long threads = asLong(row.get("threads"));
        return new BoardDto(
                asLong(row.get("id")),
                asString(row.get("title")),
                asString(row.get("description")),
                threads == null ? 0 : (int) Math.min(Integer.MAX_VALUE, threads),
                asString(row.get("tone"))
        );
    }

    private List<HomeContentDtos.ColumnDto> defaultColumns() {
        return List.of(
                new HomeContentDtos.ColumnDto(
                        1L,
                        "少女乐队观察档案",
                        "girls-band-notes",
                        "轻音编辑",
                        "围绕角色成长、舞台表现与情绪节奏做持续连载。",
                        "sunset"
                ),
                new HomeContentDtos.ColumnDto(
                        2L,
                        "设定世界观工坊",
                        "world-lore-workshop",
                        "设定考据员",
                        "整理组织谱系、时间线与关键设定，方便快速检索。",
                        "ocean"
                ),
                new HomeContentDtos.ColumnDto(
                        3L,
                        "角色塑形笔记",
                        "character-shaping-notes",
                        "角色主笔",
                        "记录服装、动机与关系变化，沉淀角色卡片资产。",
                        "violet"
                )
        );
    }

    private List<String> defaultAnnouncements() {
        return List.of(
                "后台文章管理与评论管理接口已接入联调。",
                "周五晚间专题页将更新少女乐队相关内容。",
                "用户权限体系将按管理员 / 编辑 / 审核员分层落地。"
        );
    }

    private List<HomeContentDtos.EditorialSignalDto> defaultEditorialSignals() {
        return List.of(
                new HomeContentDtos.EditorialSignalDto(
                        1L,
                        "本周主线",
                        "轻音少女专题排期中",
                        "本周聚焦校园乐队故事线与角色舞台表现分析。",
                        "进行中"
                ),
                new HomeContentDtos.EditorialSignalDto(
                        2L,
                        "内容策略",
                        "评论区审核节奏优化",
                        "针对高峰时段评论流量，调整审核优先级与批处理策略。",
                        "已启用"
                ),
                new HomeContentDtos.EditorialSignalDto(
                        3L,
                        "前端体验",
                        "首页模块持续补齐",
                        "首页公告、周排期与站点流程模块已切换为后端输出。",
                        "已完成"
                )
        );
    }

    private List<HomeContentDtos.WeeklyScheduleDto> defaultWeeklySchedule() {
        return List.of(
                new HomeContentDtos.WeeklyScheduleDto(1L, "Mon", "新番速记", "更新本周追番观察与镜头亮点。"),
                new HomeContentDtos.WeeklyScheduleDto(2L, "Wed", "设定考据", "补全组织关系、术语与时间线档案。"),
                new HomeContentDtos.WeeklyScheduleDto(3L, "Fri", "角色观察", "发布角色成长线与名场面解读。"),
                new HomeContentDtos.WeeklyScheduleDto(4L, "Sun", "原创连载", "上新原创企划章节与幕后记录。")
        );
    }

    private List<HomeContentDtos.StationProtocolDto> defaultStationProtocols() {
        return List.of(
                new HomeContentDtos.StationProtocolDto(1L, "01", "进入分区", "从番剧解析、设定考据或角色设定进入内容链路。"),
                new HomeContentDtos.StationProtocolDto(2L, "02", "沉浸阅读", "通过文章与标签继续扩展相关主题内容。"),
                new HomeContentDtos.StationProtocolDto(3L, "03", "形成讨论", "参与评论、点赞与回复，沉淀站点长期互动。")
        );
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String asPublicPath(Object value) {
        String path = asString(value).trim();
        if (path.isBlank() || path.startsWith("/") || path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        return "/" + path;
    }

    private Integer asInteger(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}
