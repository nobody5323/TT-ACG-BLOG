package nobody.utils;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;

public final class WebUtils {

    private WebUtils() {
    }

    public static void renderString(HttpServletResponse response, String string) {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try {
            response.getWriter().print(string);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to render response body", e);
        }
    }

    public static void setDownLoadHeader(String filename, HttpServletResponse response) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String safeName = filename == null ? "download.xlsx" : filename;
        String fname = URLEncoder.encode(safeName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-disposition", "attachment; filename=" + fname);
    }
}
