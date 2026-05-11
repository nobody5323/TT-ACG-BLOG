package nobody.dto.admin;

import lombok.Data;

import java.util.List;

public class AdminDashboardDtos {

    @Data
    public static class OverviewDto {
        private Long publishedPosts;
        private Long pendingComments;
        private Long activeUsers;
        private Long weeklyDrafts;
    }

    @Data
    public static class TimelineItemDto {
        private String time;
        private String text;
    }

    @Data
    public static class TimelineResponseDto {
        private List<TimelineItemDto> items;
    }
}
