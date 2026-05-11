package nobody.dto.content;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ArticlesResponseDto {

    private List<HomeContentDtos.ArticleDto> items;

    private FiltersDto filters;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FiltersDto {
        private List<HomeContentDtos.CategoryDto> categories;
        private List<HomeContentDtos.TagDto> tags;
    }
}

