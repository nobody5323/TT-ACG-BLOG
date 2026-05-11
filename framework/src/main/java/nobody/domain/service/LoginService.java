package nobody.domain.service;


import nobody.domain.entity.ResponseResult;
import nobody.dto.Login.LoginResponseDto;

public interface LoginService {
    ResponseResult<LoginResponseDto> login(String nickname, String password);
}
