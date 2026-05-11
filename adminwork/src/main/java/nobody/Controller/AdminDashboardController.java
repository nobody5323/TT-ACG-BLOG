package nobody.Controller;

import nobody.domain.entity.ResponseResult;
import nobody.domain.service.AdminDashboardService;
import nobody.dto.admin.AdminDashboardDtos;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/overview")
    public ResponseResult<AdminDashboardDtos.OverviewDto> overview() {
        return adminDashboardService.overview();
    }

    @GetMapping("/timeline")
    public ResponseResult<AdminDashboardDtos.TimelineResponseDto> timeline() {
        return adminDashboardService.timeline();
    }
}
