package nobody.domain.service.impl;

import nobody.domain.entity.ResponseResult;
import nobody.dto.admin.AdminDashboardDtos;
import nobody.domain.mapper.AdminPostMapper;
import nobody.domain.mapper.AdminCommentMapper;
import nobody.domain.mapper.AdminUserMapper;
import nobody.domain.service.AdminDashboardService;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final AdminPostMapper adminPostMapper;
    private final AdminCommentMapper adminCommentMapper;
    private final AdminUserMapper adminUserMapper;

    public AdminDashboardServiceImpl(AdminPostMapper adminPostMapper,
                                     AdminCommentMapper adminCommentMapper,
                                     AdminUserMapper adminUserMapper) {
        this.adminPostMapper = adminPostMapper;
        this.adminCommentMapper = adminCommentMapper;
        this.adminUserMapper = adminUserMapper;
    }

    @Override
    public ResponseResult<AdminDashboardDtos.OverviewDto> overview() {
        AdminDashboardDtos.OverviewDto dto = new AdminDashboardDtos.OverviewDto();
        dto.setPublishedPosts(adminPostMapper.countPosts(null, 1));
        dto.setPendingComments(adminCommentMapper.countComments(null, 0));
        dto.setActiveUsers(adminUserMapper.countUsers(null, 1));
        dto.setWeeklyDrafts(adminPostMapper.countPosts(null, 0));
        return ResponseResult.okResult(dto);
    }

    @Override
    public ResponseResult<AdminDashboardDtos.TimelineResponseDto> timeline() {
        List<AdminDashboardDtos.TimelineItemDto> items = new ArrayList<>();
        String now = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

        AdminDashboardDtos.TimelineItemDto a = new AdminDashboardDtos.TimelineItemDto();
        a.setTime(now);
        a.setText("后台服务运行正常，等待新的管理操作");
        items.add(a);

        AdminDashboardDtos.TimelineResponseDto dto = new AdminDashboardDtos.TimelineResponseDto();
        dto.setItems(items);
        return ResponseResult.okResult(dto);
    }
}
