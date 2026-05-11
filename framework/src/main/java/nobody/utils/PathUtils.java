package nobody.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class PathUtils {

    private static final DateTimeFormatter DATE_PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd/");

    private PathUtils() {
    }

    public static String generateFilePath(String fileName) {
        String datePath = LocalDate.now().format(DATE_PATH_FORMATTER);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String fileType = extractExtension(fileName);
        return datePath + uuid + fileType;
    }

    private static String extractExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "";
        }
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(index);
    }
}
