package nobody.dto.admin;

import java.util.List;

public class AdminPostDtos {

    public static class PostListResponseDto {
        private Long total;
        private List<PostItemDto> items;

        public PostListResponseDto() {
        }

        public PostListResponseDto(Long total, List<PostItemDto> items) {
            this.total = total;
            this.items = items;
        }

        public Long getTotal() {
            return total;
        }

        public void setTotal(Long total) {
            this.total = total;
        }

        public List<PostItemDto> getItems() {
            return items;
        }

        public void setItems(List<PostItemDto> items) {
            this.items = items;
        }
    }

    public static class PostItemDto {
        private Long id;
        private String slug;
        private String title;
        private String summary;
        private Integer publishStatus;
        private Integer visibility;
        private Integer isFeatured;
        private Integer isTop;
        private String publishedTime;
        private String updateTime;

        public PostItemDto() {
        }

        public PostItemDto(Long id, String slug, String title, String summary, Integer publishStatus, Integer visibility,
                           Integer isFeatured, Integer isTop, String publishedTime, String updateTime) {
            this.id = id;
            this.slug = slug;
            this.title = title;
            this.summary = summary;
            this.publishStatus = publishStatus;
            this.visibility = visibility;
            this.isFeatured = isFeatured;
            this.isTop = isTop;
            this.publishedTime = publishedTime;
            this.updateTime = updateTime;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getSlug() { return slug; }
        public void setSlug(String slug) { this.slug = slug; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        public Integer getPublishStatus() { return publishStatus; }
        public void setPublishStatus(Integer publishStatus) { this.publishStatus = publishStatus; }
        public Integer getVisibility() { return visibility; }
        public void setVisibility(Integer visibility) { this.visibility = visibility; }
        public Integer getIsFeatured() { return isFeatured; }
        public void setIsFeatured(Integer isFeatured) { this.isFeatured = isFeatured; }
        public Integer getIsTop() { return isTop; }
        public void setIsTop(Integer isTop) { this.isTop = isTop; }
        public String getPublishedTime() { return publishedTime; }
        public void setPublishedTime(String publishedTime) { this.publishedTime = publishedTime; }
        public String getUpdateTime() { return updateTime; }
        public void setUpdateTime(String updateTime) { this.updateTime = updateTime; }
    }

    public static class PostDetailDto {
        private Long id;
        private String slug;
        private Integer postType;
        private String title;
        private String summary;
        private Integer readingMinutes;
        private String coverUrl;
        private String coverTone;
        private String content;
        private Long authorId;
        private Long boardId;
        private Long categoryId;
        private Long columnId;
        private Integer publishStatus;
        private Integer visibility;
        private Integer isTop;
        private Integer isFeatured;
        private Integer ranking;

        public PostDetailDto() {
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getSlug() { return slug; }
        public void setSlug(String slug) { this.slug = slug; }
        public Integer getPostType() { return postType; }
        public void setPostType(Integer postType) { this.postType = postType; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        public Integer getReadingMinutes() { return readingMinutes; }
        public void setReadingMinutes(Integer readingMinutes) { this.readingMinutes = readingMinutes; }
        public String getCoverUrl() { return coverUrl; }
        public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
        public String getCoverTone() { return coverTone; }
        public void setCoverTone(String coverTone) { this.coverTone = coverTone; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public Long getAuthorId() { return authorId; }
        public void setAuthorId(Long authorId) { this.authorId = authorId; }
        public Long getBoardId() { return boardId; }
        public void setBoardId(Long boardId) { this.boardId = boardId; }
        public Long getCategoryId() { return categoryId; }
        public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
        public Long getColumnId() { return columnId; }
        public void setColumnId(Long columnId) { this.columnId = columnId; }
        public Integer getPublishStatus() { return publishStatus; }
        public void setPublishStatus(Integer publishStatus) { this.publishStatus = publishStatus; }
        public Integer getVisibility() { return visibility; }
        public void setVisibility(Integer visibility) { this.visibility = visibility; }
        public Integer getIsTop() { return isTop; }
        public void setIsTop(Integer isTop) { this.isTop = isTop; }
        public Integer getIsFeatured() { return isFeatured; }
        public void setIsFeatured(Integer isFeatured) { this.isFeatured = isFeatured; }
        public Integer getRanking() { return ranking; }
        public void setRanking(Integer ranking) { this.ranking = ranking; }
    }

    public static class SavePostRequestDto {
        private String slug;
        private Integer postType;
        private String title;
        private String summary;
        private Integer readingMinutes;
        private String coverUrl;
        private String coverTone;
        private String content;
        private Long boardId;
        private Long categoryId;
        private Long columnId;
        private Integer visibility;
        private Integer isTop;
        private Integer isFeatured;
        private Integer ranking;

        public SavePostRequestDto() {
        }

        public String getSlug() { return slug; }
        public void setSlug(String slug) { this.slug = slug; }
        public Integer getPostType() { return postType; }
        public void setPostType(Integer postType) { this.postType = postType; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        public Integer getReadingMinutes() { return readingMinutes; }
        public void setReadingMinutes(Integer readingMinutes) { this.readingMinutes = readingMinutes; }
        public String getCoverUrl() { return coverUrl; }
        public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
        public String getCoverTone() { return coverTone; }
        public void setCoverTone(String coverTone) { this.coverTone = coverTone; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public Long getBoardId() { return boardId; }
        public void setBoardId(Long boardId) { this.boardId = boardId; }
        public Long getCategoryId() { return categoryId; }
        public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
        public Long getColumnId() { return columnId; }
        public void setColumnId(Long columnId) { this.columnId = columnId; }
        public Integer getVisibility() { return visibility; }
        public void setVisibility(Integer visibility) { this.visibility = visibility; }
        public Integer getIsTop() { return isTop; }
        public void setIsTop(Integer isTop) { this.isTop = isTop; }
        public Integer getIsFeatured() { return isFeatured; }
        public void setIsFeatured(Integer isFeatured) { this.isFeatured = isFeatured; }
        public Integer getRanking() { return ranking; }
        public void setRanking(Integer ranking) { this.ranking = ranking; }
    }
}
