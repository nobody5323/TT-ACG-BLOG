package nobody.domain.entity;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.baomidou.mybatisplus.annotation.TableName;
/**
 * 用户表(SysUser)表实体类
 *
 * @author makejava
 * @since 2026-04-19 13:22:59
 */
@SuppressWarnings("serial")
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("sys_user")
public class SysUser  {
//主键ID@TableId
    private String id;

//用户名
    private String username;
//昵称
    private String nickname;
//密码哈希
    private String passwordHash;
//头像URL
    private String avatarUrl;
//邮箱
    private String email;
//手机号
    private String phone;
//个性签名
    private String signature;
//状态 0禁用 1启用
    private Integer status;
//最后登录时间
    private Date lastLoginTime;
//逻辑删除 0否 1是
    private Integer deleted;
//创建时间
    private Date createTime;
//更新时间
    private Date updateTime;



}


