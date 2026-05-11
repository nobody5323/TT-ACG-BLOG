package nobody.Controller;

import nobody.domain.entity.ResponseResult;
import nobody.domain.service.AdminPostService;
import nobody.dto.admin.AdminPostDtos;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/posts")
public class AdminPostController {

    private final AdminPostService adminPostService;

    public AdminPostController(AdminPostService adminPostService) {
        this.adminPostService = adminPostService;
    }

    @GetMapping
    public ResponseResult<AdminPostDtos.PostListResponseDto> list(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) Integer publishStatus,
            @RequestParam(value = "pageNum", required = false) Long pageNum,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return adminPostService.list(keyword, publishStatus, pageNum, pageSize);
    }

    @GetMapping("/{id}")
    public ResponseResult<AdminPostDtos.PostDetailDto> detail(@PathVariable("id") Long id) {
        return adminPostService.detail(id);
    }

    @PostMapping
    public ResponseResult<AdminPostDtos.PostDetailDto> createDraft(@RequestBody AdminPostDtos.SavePostRequestDto req) {
        return adminPostService.createDraft(req);
    }

    @PutMapping("/{id}")
    public ResponseResult<AdminPostDtos.PostDetailDto> update(
            @PathVariable("id") Long id,
            @RequestBody AdminPostDtos.SavePostRequestDto req) {
        return adminPostService.update(id, req);
    }

    @PostMapping("/{id}/publish")
    public ResponseResult<Void> publish(@PathVariable("id") Long id) {
        return adminPostService.publish(id);
    }

    @PostMapping("/{id}/offline")
    public ResponseResult<Void> offline(@PathVariable("id") Long id) {
        return adminPostService.offline(id);
    }

    @DeleteMapping("/{id}")
    public ResponseResult<Void> delete(@PathVariable("id") Long id) {
        return adminPostService.delete(id);
    }
}
