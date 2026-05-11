package nobody.Controller;

import nobody.domain.entity.ResponseResult;
import nobody.domain.service.AdminCommentService;
import nobody.dto.admin.AdminCommentDtos;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/comments")
public class AdminCommentController {

    private final AdminCommentService adminCommentService;

    public AdminCommentController(AdminCommentService adminCommentService) {
        this.adminCommentService = adminCommentService;
    }

    @GetMapping
    public ResponseResult<AdminCommentDtos.CommentListResponseDto> list(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "pageNum", required = false) Long pageNum,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return adminCommentService.list(keyword, status, pageNum, pageSize);
    }

    @GetMapping("/{id}")
    public ResponseResult<AdminCommentDtos.CommentItemDto> detail(@PathVariable("id") Long id) {
        return adminCommentService.detail(id);
    }

    @PostMapping("/{id}/reply")
    public ResponseResult<AdminCommentDtos.CommentItemDto> reply(
            @PathVariable("id") Long id,
            @RequestBody AdminCommentDtos.ReplyRequestDto req) {
        return adminCommentService.reply(id, req);
    }

    @PatchMapping("/{id}/status")
    public ResponseResult<Void> patchStatus(
            @PathVariable("id") Long id,
            @RequestBody AdminCommentDtos.StatusPatchRequestDto req) {
        return adminCommentService.patchStatus(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseResult<Void> delete(@PathVariable("id") Long id) {
        return adminCommentService.delete(id);
    }

    @PostMapping("/batch/status")
    public ResponseResult<Void> batchStatus(@RequestBody AdminCommentDtos.BatchStatusRequestDto req) {
        return adminCommentService.batchStatus(req);
    }
}
