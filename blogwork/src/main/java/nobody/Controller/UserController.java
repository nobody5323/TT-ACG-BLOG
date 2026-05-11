package nobody.Controller;

import nobody.domain.entity.ResponseResult;
import nobody.domain.service.UserService;
import nobody.dto.profile.UserProfileResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/user/profile")
    public ResponseResult<UserProfileResponseDto> profile(){
        return userService.profile();
    }
}
