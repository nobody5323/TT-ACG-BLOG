package nobody.dto.admin;

import lombok.Data;

import java.util.List;

public class AdminUserDtos {

    @Data
    public static class UserItemDto {
        private Long id;
        private String username;
        private String nickname;
        private String signature;
        private Integer status;
        private String lastLoginTime;
        private String createTime;
        private List<String> roles;
    }

    @Data
    public static class UserListResponseDto {
        private Long total;
        private List<UserItemDto> items;
    }

    @Data
    public static class SaveUserRequestDto {
        private String username;
        private String nickname;
        private String password;
        private String signature;
        private Integer status;
    }

    @Data
    public static class PatchRolesRequestDto {
        private List<String> roles;
    }

    @Data
    public static class PatchStatusRequestDto {
        private Integer status;
    }
}
