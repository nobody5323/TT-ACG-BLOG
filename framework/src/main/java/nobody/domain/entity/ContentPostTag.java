package nobody.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 内容标签关联表(ContentPostTag)表实体类
 *
 * @author makejava
 * @since 2026-04-19 12:56:18
 */
@SuppressWarnings("serial")
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("content_post_tag")
public class ContentPostTag  {
//主键ID@TableId
    private String id;

//内容ID
    private String postId;
//标签ID
    private String tagId;



}


