package nobody.domain.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;


/**
 * 用户表(SysUser)表数据库访问层
 *
 * @author makejava
 * @since 2026-04-19 13:22:59
 */
@Mapper
public interface SysUserMapper {

    List<Map<String, Object>> selectSimpleById(@Param("id") Long id);

    List<Map<String, Object>> selectByNickname(@Param("nickname") String nickname);

    Long countByNickname(@Param("nickname") String nickname);

    int insertUser(@Param("username") String username,
                   @Param("nickname") String nickname,
                   @Param("passwordHash") String passwordHash,
                   @Param("signature") String signature,
                   @Param("status") Integer status);

    Long lastInsertId();

    int insertUserRoleByRoleName(@Param("userId") Long userId, @Param("roleName") String roleName);

}

