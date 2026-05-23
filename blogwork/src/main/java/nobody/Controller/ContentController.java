package nobody.Controller;

import jakarta.servlet.http.HttpServletRequest;
import nobody.domain.entity.ResponseResult;
import nobody.domain.service.CommentService;
import nobody.domain.service.ContentService;
import nobody.dto.comment.CommentDto;
import nobody.dto.content.ArticlesResponseSlugDto;
import nobody.dto.content.ArticlesResponseDto;
import nobody.dto.content.HomeContentDtos;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/content")
public class ContentController {

    @Autowired
    private ContentService contentService;
    @Autowired
    private CommentService commentService;

    @GetMapping("/home")
    public ResponseResult<HomeContentDtos.HomeResponseDto> home() {
        return contentService.home();
    }

    @GetMapping("/articles")
    public ResponseResult<ArticlesResponseDto> articles(@RequestParam(value = "category", required = false) String category,
                                                        @RequestParam(value = "tag", required = false) String tag,
                                                        @RequestParam(value = "keyword", required = false) String keyword,
                                                        @RequestParam(value = "sort", required = false) String sort) {
        return contentService.articles(category, tag, keyword, sort);
    }

    @GetMapping("/articles/{slug}")
    public ResponseResult<ArticlesResponseSlugDto> slug(@PathVariable("slug") String slug){
        return contentService.slug(slug);
    }

    @GetMapping("/search")
    public ResponseResult<List<HomeContentDtos.ArticleDto>> search(@RequestParam(value = "keyword",required = false) String keyword){
        return contentService.search(keyword);
    }

    @PostMapping("/articles/{slug}/view")
    public ResponseResult<Map<String, Object>> incrementView(@PathVariable("slug") String slug) {
        return contentService.incrementView(slug);
    }

    @PostMapping("/articles/{slug}/like")
    public ResponseResult<Map<String, Object>> toggleLike(@PathVariable("slug") String slug,
                                                          @RequestHeader(value = "X-ACG-Client-Id", required = false) String clientId,
                                                          HttpServletRequest request) {
        return contentService.toggleLike(slug, resolveClientKey(clientId, request));
    }

    @GetMapping("/articles/{slug}/comments")
    public ResponseResult<CommentDto.ArticleCommentsResponseDto> comments(
            @PathVariable("slug") String slug,
            @RequestParam(value = "cursor", required = false) Long cursor,
            @RequestParam(value = "size", required = false) Integer size) {
        return commentService.comments(slug, cursor, size);
    }

    @PostMapping("/articles/{slug}/comments")
    public ResponseResult<CommentDto.CreateCommentResponseDto> createComments(
            @PathVariable("slug") String slug,
            @RequestBody Map<String, Object> req) {
        String content = req == null ? null : String.valueOf(req.getOrDefault("content", ""));
        return commentService.createComments(slug, content);
    }

    @DeleteMapping("/comments/{id}")
    public ResponseResult<Void> deleteComment(@PathVariable("id") Long id) {
        return commentService.deleteComment(id);
    }

    @PostMapping("/comments/{id}/reply")
    public ResponseResult<CommentDto.CreateCommentResponseDto> replyComment(
            @PathVariable("id") Long id,
            @RequestBody Map<String, Object> req) {
        String content = req == null ? null : String.valueOf(req.getOrDefault("content", ""));
        return commentService.replyComment(id, content);
    }

    private String resolveClientKey(String clientId, HttpServletRequest request) {
        String cleaned = clientId == null ? "" : clientId.trim();
        if (cleaned.length() >= 8 && cleaned.length() <= 80 && cleaned.matches("[A-Za-z0-9_-]+")) {
            return "client:" + cleaned;
        }

        String remoteAddr = request == null ? "" : String.valueOf(request.getRemoteAddr());
        String userAgent = request == null ? "" : String.valueOf(request.getHeader("User-Agent"));
        return "fallback:" + Integer.toHexString((remoteAddr + "|" + userAgent).hashCode());
    }
}
