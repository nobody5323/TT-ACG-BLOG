package nobody.dto.content;

import java.util.List;

public class ArticlesResponseSlugDto {

    private HomeContentDtos.ArticleDto article;
    private List<HomeContentDtos.ArticleDto> related;

    public ArticlesResponseSlugDto() {
    }

    public ArticlesResponseSlugDto(HomeContentDtos.ArticleDto article,
                                   List<HomeContentDtos.ArticleDto> related) {
        this.article = article;
        this.related = related;
    }

    public HomeContentDtos.ArticleDto getArticle() {
        return article;
    }

    public void setArticle(HomeContentDtos.ArticleDto article) {
        this.article = article;
    }

    public List<HomeContentDtos.ArticleDto> getRelated() {
        return related;
    }

    public void setRelated(List<HomeContentDtos.ArticleDto> related) {
        this.related = related;
    }
}

