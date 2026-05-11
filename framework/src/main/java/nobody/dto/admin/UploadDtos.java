package nobody.dto.admin;

import lombok.*;

import java.util.List;

public class UploadDtos {
    @Setter
    @Getter
    public static class UploadFileItemDto {
        private String originalName;
        private String storedName;
        private String contentType;
        private Long size;
        private String relativePath;

        public UploadFileItemDto() {
        }

        public UploadFileItemDto(String originalName, String storedName, String contentType, Long size, String relativePath) {
            this.originalName = originalName;
            this.storedName = storedName;
            this.contentType = contentType;
            this.size = size;
            this.relativePath = relativePath;
        }

    }

    @Setter
    @Getter
    public static class UploadBatchResponseDto {
        private List<UploadFileItemDto> items;

        public UploadBatchResponseDto() {
        }

        public UploadBatchResponseDto(List<UploadFileItemDto> items) {
            this.items = items;
        }

    }
}
