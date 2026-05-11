package nobody.domain.service;

import nobody.domain.entity.ResponseResult;
import nobody.dto.comment.CommentDto;
import nobody.dto.content.ArticlesResponseSlugDto;
import nobody.dto.content.ArticlesResponseDto;
import nobody.dto.content.HomeContentDtos;
import nobody.dto.content.HomeContentDtos.HomeResponseDto;

import java.util.List;

public interface ContentService {

    ResponseResult<HomeResponseDto> home();

    ResponseResult<ArticlesResponseDto> articles(String category, String tag, String keyword, String sort);

    ResponseResult<ArticlesResponseSlugDto> slug(String slug);

    ResponseResult<List<HomeContentDtos.ArticleDto>> search(String keyword);

}

