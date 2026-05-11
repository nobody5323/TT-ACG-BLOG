package nobody;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import nobody.domain.mapper.CommentsMapper;
import nobody.domain.mapper.ContentBoardMapper;
import nobody.domain.mapper.ContentCategoryMapper;
import nobody.domain.mapper.ContentPostMapper;
import nobody.domain.mapper.ContentPostTagMapper;
import nobody.domain.mapper.ContentTagMapper;
import nobody.domain.mapper.SysUserMapper;
import nobody.domain.mapper.UserProfileMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@OpenAPIDefinition(
        info = @Info(title = "ACG Blog API", version = "v1", description = "ACG Blog 接口文档")
)
@SpringBootApplication
@ComponentScan(
        basePackages = "nobody",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "nobody\\.domain\\.service\\.impl\\.Admin.*"
        )
)
@MapperScan(basePackageClasses = {
        ContentPostMapper.class,
        ContentCategoryMapper.class,
        ContentTagMapper.class,
        ContentBoardMapper.class,
        ContentPostTagMapper.class,
        CommentsMapper.class,
        SysUserMapper.class,
        UserProfileMapper.class
})
public class BlogApplication {
    public static void main(String[] args) {
        SpringApplication.run(BlogApplication.class, args);
    }
}
