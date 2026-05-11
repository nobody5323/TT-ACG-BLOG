package nobody.domain.service.impl;

import nobody.Enum.AppHttpCodeEnum;
import nobody.domain.service.AdminUploadService;
import nobody.dto.admin.UploadDtos;
import nobody.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AdminUploadServiceImpl implements AdminUploadService {

    // 图片单文件上限：5MB
    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    // 通用附件单文件上限：20MB
    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;
    // 图片扩展名白名单
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    // 上传根目录（相对服务运行目录）
    private static final Path BASE_UPLOAD_DIR = Paths.get("uploads", "admin");
    // 按日期分目录，便于归档和清理
    private static final DateTimeFormatter DAY_DIR = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public UploadDtos.UploadFileItemDto uploadImage(MultipartFile file) {
        // 1) 基础校验：不能为空、文件名不能为空
        validateSingleFile(file, "图片");
        String extension = extensionOf(file.getOriginalFilename());

        // 2) 白名单校验：仅允许常见图片格式
        if (!IMAGE_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BusinessException(AppHttpCodeEnum.BAD_REQUEST, "仅支持 jpg/jpeg/png/webp/gif 图片");
        }

        // 3) 大小校验
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new BusinessException(AppHttpCodeEnum.BAD_REQUEST, "图片大小不能超过 5MB");
        }

        // 4) 落盘并返回元信息
        return saveFile(file, "images");
    }

    @Override
    public List<UploadDtos.UploadFileItemDto> uploadFiles(MultipartFile[] files) {
        // 批量上传至少一个文件
        if (files == null || files.length == 0) {
            throw new BusinessException(AppHttpCodeEnum.BAD_REQUEST, "请选择要上传的文件");
        }

        List<UploadDtos.UploadFileItemDto> result = new ArrayList<>();
        for (MultipartFile file : files) {
            // 每个文件都走基础校验
            validateSingleFile(file, "文件");
            // 每个文件做大小限制
            if (file.getSize() > MAX_FILE_SIZE) {
                throw new BusinessException(AppHttpCodeEnum.BAD_REQUEST, "单个文件大小不能超过 20MB");
            }
            result.add(saveFile(file, "files"));
        }
        return result;
    }

    private void validateSingleFile(MultipartFile file, String typeName) {
        // 防空对象/空内容
        if (file == null || file.isEmpty()) {
            throw new BusinessException(AppHttpCodeEnum.BAD_REQUEST, typeName + "不能为空");
        }
        // 防空文件名
        if (file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()) {
            throw new BusinessException(AppHttpCodeEnum.BAD_REQUEST, typeName + "文件名不能为空");
        }
    }

    private UploadDtos.UploadFileItemDto saveFile(MultipartFile file, String category) {
        String originalName = file.getOriginalFilename();
        String extension = extensionOf(originalName);
        // 统一用 UUID 重命名，避免同名覆盖
        String storedName = UUID.randomUUID().toString().replace("-", "")
                + (extension.isEmpty() ? "" : "." + extension);
        String day = LocalDate.now().format(DAY_DIR);

        // 目标目录：uploads/admin/{category}/{yyyyMMdd}
        Path dir = BASE_UPLOAD_DIR.resolve(category).resolve(day);
        Path target = dir.resolve(storedName);
        try {
            Files.createDirectories(dir);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            // 统一抛业务异常，交给全局异常处理器输出标准响应
            throw new BusinessException(AppHttpCodeEnum.SYSTEM_ERROR, "文件保存失败");
        }

        // 返回相对路径，后续可由静态资源映射或网关转为可访问 URL
        String relativePath = BASE_UPLOAD_DIR.resolve(category).resolve(day).resolve(storedName)
                .toString()
                .replace("\\", "/");
        return new UploadDtos.UploadFileItemDto(
                originalName,
                storedName,
                file.getContentType() == null ? "" : file.getContentType(),
                file.getSize(),
                relativePath
        );
    }

    private String extensionOf(String filename) {
        // 取最后一个 '.' 之后的扩展名；没有则返回空串
        if (filename == null) {
            return "";
        }
        int index = filename.lastIndexOf('.');
        if (index < 0 || index == filename.length() - 1) {
            return "";
        }
        return filename.substring(index + 1);
    }
}
