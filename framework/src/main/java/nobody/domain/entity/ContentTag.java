package nobody.domain.entity;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.baomidou.mybatisplus.annotation.TableName;
/**
 * 标签表(ContentTag)表实体类
 *
 * @author makejava
 * @since 2026-04-19 13:22:59
 */
@SuppressWarnings("serial")
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("content_tag")
public class ContentTag  {
//主键ID@TableId
    private String id;

//标签编码
    private String tagCode;
//标签名称
    private String tagName;
//热度值
    private Integer heat;
//状态 0禁用 1启用
    private Integer status;
//创建时间
    private Date createTime;
//更新时间
    private Date updateTime;



}


