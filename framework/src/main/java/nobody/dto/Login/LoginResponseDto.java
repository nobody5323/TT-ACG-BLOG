package nobody.dto.Login;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponseDto {

    private String token;
    private UserInfoDto user;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserInfoDto implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private Long id;
        private String nickname;
        private String level;
        private String signature;
    }
}

