CREATE TABLE IF NOT EXISTS content_like (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Like ID',
  user_id BIGINT UNSIGNED NULL COMMENT 'Logged-in user ID when available',
  client_id VARCHAR(96) NOT NULL COMMENT 'Anonymous client key',
  post_id BIGINT UNSIGNED NOT NULL COMMENT 'Post ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  PRIMARY KEY (id),
  UNIQUE KEY uk_content_like_client_post (client_id, post_id),
  KEY idx_content_like_user_id (user_id),
  KEY idx_content_like_post_id (post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Post likes';
