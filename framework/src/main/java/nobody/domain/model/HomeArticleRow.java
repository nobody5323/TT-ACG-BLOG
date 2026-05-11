package nobody.domain.model;


public record HomeArticleRow(
        Long id,
        String slug,
        String title,
        String summary,
        String category,
        String categorySlug,
        String author,
        String publishedAt,
        Integer readingMinutes,
        String coverTone,
        Integer featured,
        Integer ranking,
        Integer views,
        Integer likes,
        Integer favorites,
        String column
) {
}

