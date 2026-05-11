package nobody.Controller;

import nobody.domain.entity.ResponseResult;
import nobody.domain.service.LoginService;
import nobody.dto.Login.LoginRequestDto;
import nobody.dto.Login.LoginResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class LoginController {

    @Autowired
    private LoginService loginService;

    @PostMapping("/auth/login")
    public ResponseResult<LoginResponseDto> login(@RequestBody LoginRequestDto req){
        return loginService.login(req.getNickname(), req.getPassword());
    }
}
