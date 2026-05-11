package nobody.domain.service;

import nobody.domain.entity.ResponseResult;
import nobody.dto.profile.UserProfileResponseDto;

public interface UserService {

    ResponseResult<UserProfileResponseDto> profile();
}
