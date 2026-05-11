package nobody.config;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nobody.utils.JwtUtil;
import nobody.utils.RedisCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final RedisCache redisCache;

    public JwtAuthenticationFilter(RedisCache redisCache) {
        this.redisCache = redisCache;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = auth.substring(7);
        try {
            Claims claims = JwtUtil.parseJWT(token);
            Long userId = Long.valueOf(claims.getSubject());

            Object loginUser;
            try {
                loginUser = redisCache.getCacheObject("login:" + userId);
            } catch (Exception e) {
                log.warn("Failed to read login session from Redis. userId={}", userId, e);
                filterChain.doFilter(request, response);
                return;
            }
            if (loginUser == null) {
                filterChain.doFilter(request, response);
                return;
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception ignored) {
            // token 非法/过期，直接放行到后续鉴权失败逻辑
        }

        filterChain.doFilter(request, response);
    }
}

