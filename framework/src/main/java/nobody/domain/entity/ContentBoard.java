package nobody.domain.entity;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.baomidou.mybatisplus.annotation.TableName;
/**
 * 内容板块表(ContentBoard)表实体类
 *
 * @author makejava
 * @since 2026-04-19 13:22:59
 */
@SuppressWarnings("serial")
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("content_board")
public class ContentBoard  {
//主键ID@TableId
    private String id;

//板块编码
    private String boardCode;
//板块名称
    private String boardName;
//板块描述
    private String description;
//图标URL
    private String iconUrl;
//主题色
    private String themeColor;
//排序号
    private Integer sortNo;
//状态 0禁用 1启用
    private Integer status;
//创建时间
    private Date createTime;
//更新时间
    private Date updateTime;



}


