package nobody.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;
import java.util.Optional;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Object getLoginUser() {
        Authentication authentication = getAuthentication();
        return authentication == null ? null : authentication.getPrincipal();
    }

    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public static Boolean isAdmin() {
        Long id = getUserId();
        return id != null && id.equals(1L);
    }

    public static Long getUserId() {
        Object principal = getLoginUser();
        if (principal == null) {
            return null;
        }
        return extractUserId(principal).orElse(null);
    }

    private static Optional<Long> extractUserId(Object principal) {
        if (principal instanceof Number number) {
            return Optional.of(number.longValue());
        }

        // Try principal.getId()
        Optional<Long> directId = invokeLongMethod(principal, "getId");
        if (directId.isPresent()) {
            return directId;
        }

        // Try principal.getUser().getId()
        try {
            Method getUser = principal.getClass().getMethod("getUser");
            Object user = getUser.invoke(principal);
            if (user != null) {
                return invokeLongMethod(user, "getId");
            }
        } catch (Exception ignored) {
            return Optional.empty();
        }

        return Optional.empty();
    }

    private static Optional<Long> invokeLongMethod(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            Object value = method.invoke(target);
            if (value instanceof Number number) {
                return Optional.of(number.longValue());
            }
        } catch (Exception ignored) {
            return Optional.empty();
        }
        return Optional.empty();
    }
}
