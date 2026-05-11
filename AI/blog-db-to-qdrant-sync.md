# 博客数据库文章读取与同步方案

## 1. 目标

目标不是在用户提问时直接查询博客业务数据库全文，而是把博客数据库中的文章同步成可检索知识：

`Blog DB -> Normalize -> Markdown -> Chunk -> Embedding -> Qdrant`

这样做的目的：

1. 在线问答延迟更低
2. 检索链路更稳定
3. 不依赖博客数据库复杂查询
4. 便于和上传文件、公用知识、用户记忆统一检索

---

## 2. 基本原则

博客数据库是内容源，不是在线问答时的主检索层。

职责划分应该固定为：

- 博客数据库：保存原始文章、分类、标签、状态
- Markdown 中间层：保存标准化后的可审查文本
- Qdrant：保存切片后的检索索引

在线问答时：

1. 先查 `Qdrant`
2. 命中后拿到 `source_id`
3. 必要时再回源博客库或文章页面

不要在问答时直接 `SELECT content FROM posts WHERE ...` 做全文匹配。

---

## 3. 需要读取哪些表

具体表名取决于你的博客系统，但一般至少涉及这些逻辑对象：

### 3.1 文章主表

典型字段：

- `id`
- `title`
- `slug`
- `summary`
- `content_markdown` 或 `content_html` 或 `content`
- `status`
- `visibility`
- `author_id`
- `published_at`
- `updated_at`
- `deleted_at`

### 3.2 标签关联表

如果标签是多对多关系，通常还需要：

- `post_tags`
- `tags`

目标是最终能拿到：

- 标签名列表
- 标签 ID 列表

### 3.3 分类表

如果博客有分类，还需要：

- `categories`
- 或文章表上的 `category_id`

### 3.4 可选扩展表

如果有专门的 SEO、摘要、系列信息，也可以接入：

- `excerpt`
- `series_id`
- `cover_image_alt`
- `language`

第一版不用读太多，先把正文同步打通。

---

## 4. 第一版建议读取字段

第一版同步任务建议只取这些字段：

```text
id
title
slug
summary
content_markdown / content_html / content
status
visibility
published_at
updated_at
deleted_at
author_id
tags
category
```

### 4.1 状态过滤

只允许进入知识库的文章必须满足：

- `status = published`
- `visibility = public`
- `deleted_at IS NULL`

如果你的系统没有 `visibility`，那就至少确保只同步公开可访问文章。

### 4.2 时间过滤

做增量同步时按：

- `updated_at > last_sync_time`

这样不需要每次全量扫描所有文章。

---

## 5. 推荐查询方式

## 5.1 全量初始化

第一次建索引时跑全量：

```sql
SELECT
  p.id,
  p.title,
  p.slug,
  p.summary,
  p.content_markdown,
  p.content_html,
  p.status,
  p.visibility,
  p.author_id,
  p.published_at,
  p.updated_at,
  p.deleted_at
FROM posts p
WHERE p.status = 'published'
  AND p.deleted_at IS NULL
ORDER BY p.id ASC;
```

### 5.2 增量同步

后续定时任务跑增量：

```sql
SELECT
  p.id,
  p.title,
  p.slug,
  p.summary,
  p.content_markdown,
  p.content_html,
  p.status,
  p.visibility,
  p.author_id,
  p.published_at,
  p.updated_at,
  p.deleted_at
FROM posts p
WHERE p.updated_at > :last_sync_time
ORDER BY p.updated_at ASC, p.id ASC
LIMIT :batch_size;
```

### 5.3 标签与分类

标签和分类可以分两种方式做：

1. 主查询 join 出来
2. 先拉文章，再批量查标签映射

如果 join 后容易产生重复行，优先用第二种。

---

## 6. 同步游标表

必须单独建一个同步状态表，不要只靠程序内存记录。

建议建表：

`rag_sync_state`

字段建议：

- `source_name`
- `last_sync_time`
- `last_source_id`
- `last_success_at`
- `last_status`
- `last_error`

### 6.1 为什么要有这个表

它解决几个问题：

- 程序重启后还能继续增量同步
- 可以审计同步是否失败
- 可以知道最后同步到哪一篇文章

### 6.2 推荐游标规则

以 `updated_at + id` 双游标更稳：

- 先按 `updated_at ASC`
- 再按 `id ASC`

这样即使同一秒有多篇文章更新，也不容易漏。

---

## 7. 从数据库记录转成标准 Markdown

数据库字段不能直接拿去切片，先要统一成标准文档。

## 7.1 标准文档结构

建议每篇文章组装成这种结构：

```markdown
# {{title}}

URL: /posts/{{slug}}
PublishedAt: {{published_at}}
Tags: tag1, tag2
Category: {{category}}

## Summary

{{summary}}

## Content

{{normalized_content}}
```

### 7.2 如果正文是 Markdown

如果数据库里已经有 `content_markdown`：

- 直接做清洗
- 不要重复转格式

### 7.3 如果正文是 HTML

如果数据库里是 `content_html`：

1. 先转 Markdown
2. 再做结构清洗
3. 去掉导航、按钮、脚注噪声

### 7.4 清洗规则

至少做：

- 去多余空行
- 去前端残留控件文本
- 去复制产生的重复段落
- 统一标题层级
- 表格转文本摘要
- 图片保留 alt/说明

---

## 8. Qdrant 中如何存文章

文章不会整篇存成一条向量，而是切片后多条写入。

### 8.1 collection

文章写入：

- `blog_knowledge`

### 8.2 每个 chunk 的 payload

建议至少有这些字段：

- `source_type=blog_post`
- `source_id=文章ID`
- `source_name=文章标题`
- `slug`
- `url_path`
- `chunk_id`
- `chunk_index`
- `section_path`
- `summary`
- `tags`
- `category`
- `owner_scope=public`
- `persona_scope`
- `published_at`
- `updated_at`
- `is_approved=true`

### 8.3 为什么要保存这些字段

这样后面可以做：

- 标签过滤
- 分类过滤
- 时间范围过滤
- 来源标题展示
- 命中后回源文章页

---

## 9. 切片策略

博客文章切片不要只按长度硬切。

推荐顺序：

1. 按标题分段
2. 按自然段聚合
3. 超长段落再按 token 拆分
4. 保留 overlap

### 9.1 每个 chunk 的推荐内容

每个 chunk 最好包含：

- 当前段落正文
- 所属标题路径
- 必要时拼上文章标题

这样 rerank 时更容易命中真正有用的片段。

---

## 10. 同步流程

推荐定时任务流程如下：

1. 读取 `rag_sync_state`
2. 从博客数据库按游标拉增量文章
3. 读取对应标签和分类
4. 转成标准 Markdown
5. 清洗和切片
6. 用 `Qwen/Qwen3-Embedding-4B` 生成向量
7. 删除该文章旧的 chunk
8. 把新的 chunk 批量写入 `Qdrant`
9. 更新 `rag_sync_state`

### 10.1 为什么要先删旧 chunk 再写新 chunk

因为文章一旦改动，原切片边界和向量通常都会变。

最稳的方式是：

- 以 `source_id=文章ID` 删除旧记录
- 再整篇重建

不要尝试对单个 chunk 做复杂 diff 更新，第一版没必要。

---

## 11. 删除、下线、私密文章怎么处理

这是很容易漏掉的部分。

### 11.1 文章被删除

如果 `deleted_at IS NOT NULL`：

- 从 `Qdrant` 删除 `source_id=该文章ID` 的全部 chunk

### 11.2 文章改为草稿

如果 `status != published`：

- 也要从 `Qdrant` 删除

### 11.3 文章改为私密

如果 `visibility != public`：

- 也必须从 `Qdrant` 删除

所以增量同步不只是“新增和更新”，还必须处理“撤回可检索性”。

---

## 12. 在线问答时怎么用这些文章

在线问答时不要再直接查博客数据库正文。

正确流程：

1. 用户问题进入检索链路
2. 用 `Qwen/Qwen3-Embedding-4B` 做 recall
3. 从 `blog_knowledge` 召回文章 chunk
4. 用 `Qwen/Qwen3-Reranker-4B` 做 rerank
5. 把 topN chunk 交给 `deepseek-v4-flash`

### 12.1 什么时候回源博客数据库

只有这些情况建议回源：

- 需要拿文章最新标题或状态
- 需要返回文章链接
- 需要展示文章更多元信息

回源是展示增强，不是主检索手段。

---

## 13. 推荐模块划分

如果后面开始写代码，建议拆成这几个模块：

### 13.1 数据源读取

- `app/ingestion/blog_source`

职责：

- 连接博客数据库
- 拉文章主表
- 拉标签与分类
- 按游标做增量同步

### 13.2 内容标准化

- `app/ingestion/normalize`

职责：

- HTML 转 Markdown
- Markdown 清洗
- 标准文档拼装

### 13.3 切片与向量化

- `app/ingestion/chunking`
- `app/models`

职责：

- 标题切片
- overlap 控制
- `Qwen/Qwen3-Embedding-4B` 向量化

### 13.4 索引写入

- `app/storage/qdrant`

职责：

- 删除旧 chunk
- 批量 upsert 新 chunk
- collection 管理

### 13.5 同步任务

- `app/ingestion/jobs`

职责：

- 定时任务调度
- 游标推进
- 错误重试
- 状态落盘

---

## 14. 第一版必须做的保护

至少加上这些保护：

- 单次批量限制
- 同步失败重试
- 文章正文为空时跳过
- Markdown 转换失败时记录错误
- Qdrant 写入失败时不推进游标

### 14.1 很重要的一条

只有当一批文章全部成功写入 `Qdrant` 后，才更新 `last_sync_time`。

否则会出现“数据库文章已经读过，但索引其实没写进去”的丢数问题。

---

## 15. 最终建议

对于“要能读取博客中数据库中的文章”这件事，推荐最终做法是：

1. 从博客数据库按增量拉取公开已发布文章
2. 转成统一 Markdown 文档
3. 切片并用 `Qwen/Qwen3-Embedding-4B` 向量化
4. 写入 `Qdrant` 的 `blog_knowledge`
5. 在线问答只查 `Qdrant`
6. 用 `source_id` 和 `slug` 保留回源能力

这样这套博客问答系统才会稳定，而且能和 `Qwen/Qwen3-Reranker-4B`、用户记忆检索、上传文件检索统一到一条主链路里。
