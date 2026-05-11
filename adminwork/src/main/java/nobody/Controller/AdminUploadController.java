package nobody.Controller;

import nobody.domain.entity.ResponseResult;
import nobody.domain.service.AdminUploadService;
import nobody.dto.admin.UploadDtos;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/upload")
public class AdminUploadController {

    private final AdminUploadService adminUploadService;

    public AdminUploadController(AdminUploadService adminUploadService) {
        this.adminUploadService = adminUploadService;
    }

    @PostMapping("/image")
    public ResponseResult<UploadDtos.UploadFileItemDto> uploadImage(@RequestParam("file") MultipartFile file) {
        return ResponseResult.okResult(adminUploadService.uploadImage(file));
    }

    @PostMapping("/file")
    public ResponseResult<UploadDtos.UploadBatchResponseDto> uploadFile(@RequestParam("file") MultipartFile[] files) {
        List<UploadDtos.UploadFileItemDto> items = adminUploadService.uploadFiles(files);
        return ResponseResult.okResult(new UploadDtos.UploadBatchResponseDto(items));
    }
}
