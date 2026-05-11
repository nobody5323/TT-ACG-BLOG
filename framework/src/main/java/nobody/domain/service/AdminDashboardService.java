package nobody.domain.service;

import nobody.domain.entity.ResponseResult;
import nobody.dto.admin.AdminDashboardDtos;

public interface AdminDashboardService {
    ResponseResult<AdminDashboardDtos.OverviewDto> overview();
    ResponseResult<AdminDashboardDtos.TimelineResponseDto> timeline();
}
