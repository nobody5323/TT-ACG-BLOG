package nobody.domain.entity;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.baomidou.mybatisplus.annotation.TableName;
/**
 * 内容主表(ContentPost)表实体类
 *
 * @author makejava
 * @since 2026-04-19 13:22:59
 */
@SuppressWarnings("serial")
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("content_post")
public class ContentPost  {
//主键ID@TableId
    private String id;

//文章slug
    private String slug;
//内容类型 1文章 2帖子 3连载
    private Integer postType;
//标题
    private String title;
//摘要
    private String summary;
//阅读分钟数
    private String readingMinutes;
//封面URL
    private String coverUrl;
//封面主题色调
    private String coverTone;
//正文内容
    private String content;
//作者ID
    private String authorId;
//板块ID
    private String boardId;
//分类ID
    private String categoryId;
//专栏ID
    private String columnId;
//发布状态 0草稿 1已发布 2下线
    private Integer publishStatus;
//可见性 1公开 2私有
    private Integer visibility;
//是否置顶 0否 1是
    private Integer isTop;
//是否推荐 0否 1是
    private Integer isFeatured;
//排行值(可选)
    private Integer ranking;
//评论数
    private String commentCount;
//点赞数
    private String likeCount;
//收藏数
    private String favoriteCount;
//浏览数
    private String viewCount;
//发布时间
    private Date publishedTime;
//逻辑删除 0否 1是
    private Integer deleted;
//创建时间
    private Date createTime;
//更新时间
    private Date updateTime;



}


