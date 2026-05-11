package nobody.domain.entity;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.baomidou.mybatisplus.annotation.TableName;
/**
 * 内容分类表(ContentCategory)表实体类
 *
 * @author makejava
 * @since 2026-04-19 13:22:59
 */
@SuppressWarnings("serial")
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("content_category")
public class ContentCategory  {
//主键ID@TableId
    private String id;

//分类编码
    private String categoryCode;
//分类名称
    private String categoryName;
//分类描述
    private String description;
//分类视觉accent
    private String accent;
//排序号
    private Integer sortNo;
//状态 0禁用 1启用
    private Integer status;
//创建时间
    private Date createTime;
//更新时间
    private Date updateTime;



}


