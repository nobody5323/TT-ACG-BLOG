package nobody.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import nobody.dto.content.HomeContentDtos;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileResponseDto {
    private Long id;
    private String nickname;
    private String level;
    private String signature;
    private StatsDto stats;
    private List<HomeContentDtos.ArticleDto> favorites;
    private List<HomeContentDtos.ArticleDto> history;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StatsDto {
        private Integer favorites;
        private Integer comments;
        private Integer history;
    }
}
