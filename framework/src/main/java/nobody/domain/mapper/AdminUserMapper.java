package nobody.domain.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface AdminUserMapper {

    Long countUsers(@Param("keyword") String keyword, @Param("status") Integer status);

    List<Map<String, Object>> selectUsers(@Param("keyword") String keyword,
                                          @Param("status") Integer status,
                                          @Param("offset") Long offset,
                                          @Param("size") Integer size);

    Map<String, Object> selectUserById(@Param("id") Long id);

    int insertUser(@Param("username") String username,
                   @Param("nickname") String nickname,
                   @Param("passwordHash") String passwordHash,
                   @Param("signature") String signature,
                   @Param("status") Integer status);

    Long lastInsertId();

    int updateUser(@Param("id") Long id,
                   @Param("username") String username,
                   @Param("nickname") String nickname,
                   @Param("signature") String signature,
                   @Param("status") Integer status);

    int updateUserStatus(@Param("id") Long id, @Param("status") Integer status);

    int updateUserPassword(@Param("id") Long id, @Param("passwordHash") String passwordHash);

    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

    int deleteUserRolesByUserId(@Param("userId") Long userId);

    int insertUserRoleByCode(@Param("userId") Long userId, @Param("roleCode") String roleCode);
}
