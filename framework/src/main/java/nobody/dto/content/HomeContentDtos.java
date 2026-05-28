package nobody.dto.content;

import java.util.List;

public final class HomeContentDtos {

    private HomeContentDtos() {
    }

    public record HomeResponseDto(
            HeroDto hero,
            List<ArticleDto> featuredArticles,
            List<CategoryDto> hotCategories,
            List<TagDto> hotTags,
            List<ArticleDto> latestArticles,
            List<ColumnDto> columns,
            List<ArticleDto> rankings,
            List<CharacterDto> fandomCharacters,
            List<BoardDto> boards,
            List<String> announcements,
            List<EditorialSignalDto> editorialSignals,
            List<WeeklyScheduleDto> weeklySchedule,
            List<StationProtocolDto> stationProtocols
    ) {
    }

    public record HeroDto(
            String eyebrow,
            String title,
            String subtitle,
            ArticleDto featured,
            List<HeroMetricDto> metrics
    ) {
    }

    public record HeroMetricDto(
            String label,
            String value
    ) {
    }

    public record ArticleDto(
            Long id,
            String slug,
            String title,
            String summary,
            String category,
            String categorySlug,
            List<String> tags,
            String author,
            String publishedAt,
            Integer readingMinutes,
            String coverUrl,
            String coverTone,
            Boolean featured,
            Integer ranking,
            Integer views,
            Integer likes,
            Integer favorites,
            String column,
            List<ArticleSectionDto> content
    ) {
    }

    public record ArticleSectionDto(
            String heading,
            List<String> paragraphs
    ) {
    }

    public record CategoryDto(
            Long id,
            String name,
            String slug,
            String description,
            String accent
    ) {
    }

    public record TagDto(
            Long id,
            String name,
            Integer heat
    ) {
    }

    public record ColumnDto(
            Long id,
            String title,
            String slug,
            String persona,
            String description,
            String theme
    ) {
    }

    public record CharacterDto(
            Long id,
            String name,
            String universe,
            String role,
            String vibe,
            String tone,
            String quote,
            String image
    ) {
    }

    public record BoardDto(
            Long id,
            String title,
            String description,
            Integer threads,
            String tone
    ) {
    }

    public record EditorialSignalDto(
            Long id,
            String label,
            String title,
            String description,
            String status
    ) {
    }

    public record WeeklyScheduleDto(
            Long id,
            String day,
            String title,
            String description
    ) {
    }

    public record StationProtocolDto(
            Long id,
            String step,
            String title,
            String description
    ) {
    }
}
