package nobody.domain.service;

import nobody.dto.admin.UploadDtos;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AdminUploadService {

    UploadDtos.UploadFileItemDto uploadImage(MultipartFile file);

    List<UploadDtos.UploadFileItemDto> uploadFiles(MultipartFile[] files);
}
