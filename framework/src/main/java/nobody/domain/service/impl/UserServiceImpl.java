package nobody.domain.service.impl;

import nobody.Enum.AppHttpCodeEnum;
import nobody.domain.entity.ResponseResult;
import nobody.domain.mapper.UserProfileMapper;
import nobody.domain.service.UserService;
import nobody.dto.content.HomeContentDtos;
import nobody.dto.profile.UserProfileResponseDto;
import nobody.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    private final UserProfileMapper userProfileMapper;

    public UserServiceImpl(UserProfileMapper userProfileMapper) {
        this.userProfileMapper = userProfileMapper;
    }

    @Override
    public ResponseResult<UserProfileResponseDto> profile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException(AppHttpCodeEnum.UNAUTHORIZED);
        }

        Long userId;
        try {
            userId = Long.valueOf(String.valueOf(authentication.getPrincipal()));
        } catch (Exception e) {
            throw new BusinessException(AppHttpCodeEnum.UNAUTHORIZED, "登录状态无效");
        }

        Map<String, Object> user = userProfileMapper.selectProfileBaseByUserId(userId);
        if (user == null || user.isEmpty()) {
            throw new BusinessException(AppHttpCodeEnum.NOT_FOUND, "用户不存在");
        }

        Map<String, Object> statsMap = userProfileMapper.selectProfileStatsByUserId(userId);
        List<Map<String, Object>> favoriteRows = userProfileMapper.selectFavoriteArticlesByUserId(userId, 20);
        List<Map<String, Object>> historyRows = userProfileMapper.selectHistoryArticlesByUserId(userId, 20);

        UserProfileResponseDto dto = new UserProfileResponseDto();
        dto.setId(userId);
        dto.setNickname(asString(user.get("nickname")));
        dto.setLevel(asString(user.get("level")));
        dto.setSignature(asString(user.get("signature")));

        UserProfileResponseDto.StatsDto stats = new UserProfileResponseDto.StatsDto();
        stats.setFavorites(asInt(statsMap.get("favorites")));
        stats.setComments(asInt(statsMap.get("comments")));
        stats.setHistory(asInt(statsMap.get("history")));
        dto.setStats(stats);

        dto.setFavorites(mapArticles(favoriteRows));
        dto.setHistory(mapArticles(historyRows));

        return ResponseResult.okResult(dto);
    }

    private List<HomeContentDtos.ArticleDto> mapArticles(List<Map<String, Object>> rows) {
        return rows.stream().map(r -> new HomeContentDtos.ArticleDto(
                asLong(r.get("id")),
                asString(r.get("slug")),
                asString(r.get("title")),
                asString(r.get("summary")),
                asString(r.get("category")),
                asString(r.get("categorySlug")),
                List.of(),
                asString(r.get("author")),
                asString(r.get("publishedAt")),
                asInt(r.get("readingMinutes")),
                asPublicPath(r.get("coverUrl")),
                asString(r.get("coverTone")),
                asInt(r.get("featured")) == 1,
                asInt(r.get("ranking")),
                asInt(r.get("views")),
                asInt(r.get("likes")),
                asInt(r.get("favorites")),
                asString(r.get("column")),
                List.of()
        )).toList();
    }

    private String asString(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private String asPublicPath(Object o) {
        String path = asString(o).trim();
        if (path.isBlank() || path.startsWith("/") || path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        return "/" + path;
    }

    private Integer asInt(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number n) {
            return n.intValue();
        }
        return Integer.parseInt(String.valueOf(o));
    }

    private Long asLong(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(String.valueOf(o));
    }
}
