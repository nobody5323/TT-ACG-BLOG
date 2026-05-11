# 二次元角色博客问答 AI 架构（DeepSeek V4 Flash + Qwen3-Embedding-4B + Qwen3-Reranker-4B + Qdrant）

## 1. 目标

当前方案改为以下固定技术栈：

- 生成模型：`deepseek-v4-flash`
- 向量模型：`Qwen/Qwen3-Embedding-4B`
- 重排模型：`Qwen/Qwen3-Reranker-4B`
- 向量数据库：`Qdrant`
- 检索增强：加入独立 `rerank` 阶段

系统仍然满足四个核心目标：

1. 保持固定二次元角色风格输出
2. 基于博客知识库和上传文件做有依据的问答
3. 支持文件转 Markdown 后持续入库
4. 支持每个用户独立记忆隔离

---

## 2. 最终技术选型

### 2.1 模型职责划分

- `deepseek-v4-flash`
  - 用户对话主模型
  - 意图识别
  - Query rewrite
  - 回答生成
  - 简单摘要与记忆提炼

- `Qwen/Qwen3-Embedding-4B`
  - 文档切片向量化
  - 查询向量化
  - 公共知识库与用户记忆统一 embedding 空间

- `Qdrant`
  - 存储博客文档切片向量
  - 存储用户私有记忆向量
  - 基于 payload 做精确过滤

- `Qwen/Qwen3-Reranker-4B`
  - 对初召回结果二次排序
  - 在送入大模型前压缩到高价值上下文

### 2.2 为什么这样组合

- `deepseek-v4-flash` 适合高频问答链路，延迟和成本更适合作为博客在线交互主模型
- `Qwen3-Embedding-4B` 负责中文语义检索更合理，适合博客、设定文档、上传资料混合检索
- `Qdrant` 的 payload filter 很适合做 `user_id`、`persona_id`、`owner_scope` 隔离
- `Qwen/Qwen3-Reranker-4B` 能明显改善“向量相似但回答不需要”的误召回问题，比单纯增大 topK 更稳

---

## 3. 系统分层

### 3.1 接入层

负责：

- 博客前台问答接口
- 登录态识别
- 文件上传接口
- 管理端知识库维护接口

### 3.2 编排层

建议继续使用 `LangGraph`，负责：

- 会话状态管理
- 检索分支路由
- 记忆读写控制
- ReAct 动作调度
- 失败重试与可观测性

### 3.3 检索层

拆成四段：

1. Filter
2. Vector Recall
3. Rerank
4. Context Assembly

这是本次调整后的核心变化，`rerank` 不再是可选优化，而是默认主链路的一部分。

### 3.4 生成层

由 `deepseek-v4-flash` 完成：

- 问题理解
- 多路证据融合
- 角色风格化表达
- 不足证据时降级回答

### 3.5 存储层

至少包括：

- 业务库：用户、会话、消息、上传任务
- 对象存储：原始文件、转换后的 Markdown
- `Qdrant`：知识切片与用户记忆向量

---

## 4. Qdrant 集合设计

建议最少拆成两个 collection：

### 4.1 `blog_knowledge`

用于公共知识：

- 博客文章
- 角色设定
- FAQ
- 运营上传资料

建议 payload：

- `doc_id`
- `chunk_id`
- `source_type`
- `source_name`
- `owner_scope`
- `persona_scope`
- `tags`
- `section_path`
- `created_at`
- `updated_at`
- `access_level`
- `is_approved`

### 4.2 `user_memory`

用于每个用户的私有记忆：

- 长期偏好
- 稳定事实
- 会话摘要
- 互动策略

建议 payload：

- `memory_id`
- `user_id`
- `persona_id`
- `memory_type`
- `importance`
- `created_at`
- `updated_at`
- `confirmed`

### 4.3 隔离原则

- 公共知识检索前过滤 `owner_scope = public`
- 私有记忆检索前过滤 `user_id = 当前用户`
- 如有多角色，附加过滤 `persona_id`
- 不允许先召回再靠模型“自己判断”是否属于当前用户

---

## 5. 文件入库链路

### 5.1 上传

支持：

- PDF
- DOCX
- TXT
- Markdown
- HTML
- OCR 后文本

### 5.2 标准化

统一转换为：

1. 清洗后的 Markdown
2. 结构化 JSON

保留：

- 标题层级
- 段落结构
- 列表
- 表格摘要
- 图片说明

### 5.3 切片

采用“结构优先，长度兜底”：

- 先按标题分段
- 再按自然段合并
- 超长内容再按 token 切片
- 相邻切片保留 overlap

每个 chunk 至少带：

- `chunk_text`
- `section_path`
- `source_name`
- `owner_scope`
- `persona_scope`
- `chunk_index`

### 5.4 向量化与入库

统一使用 `Qwen/Qwen3-Embedding-4B`：

- 文档 chunk embedding
- 查询 embedding
- 用户记忆 embedding

向量写入 `Qdrant`，原始 Markdown 保存在对象存储，二者通过 `doc_id` / `chunk_id` 关联。

---

## 6. 检索链路

这是最终建议的默认在线链路：

1. 用户提问
2. `deepseek-v4-flash` 做意图识别和 query rewrite
3. 根据路由决定是否检索公共知识、私有记忆或两者都检索
4. 用 `Qwen/Qwen3-Embedding-4B` 对 query 生成向量
5. 在 `Qdrant` 做 metadata filter + vector recall
6. 合并候选结果
7. 进入 `rerank`
8. 选出最终 topN 证据
9. 组装上下文
10. `deepseek-v4-flash` 生成最终回答

### 6.1 初召回建议

- 公共知识：`topK = 12~20`
- 私有记忆：`topK = 6~10`

不要一开始就直接把召回结果塞给主模型，必须先过 rerank。

### 6.2 Rerank 的位置

`Qwen/Qwen3-Reranker-4B` 放在“向量召回之后、上下文拼装之前”。

职责：

- 删除语义近但不回答问题的片段
- 提升标题命中、人物设定命中、事实片段命中
- 让公共知识与私有记忆进入统一排序

### 6.3 Rerank 输入

输入结构建议统一成：

- `query`
- `candidate_text`
- `candidate_source`
- `candidate_type`
- `candidate_metadata`

其中 `candidate_type` 至少区分：

- `public_doc`
- `user_memory`
- `session_summary`

### 6.4 Rerank 输出

输出保留：

- `candidate_id`
- `rerank_score`
- `source_type`
- `payload`

最终给大模型的上下文建议只保留：

- 公共知识 `top 4~6`
- 私有记忆 `top 2~4`

---

## 7. Rerank 策略

本次要求中“加入 rerank”建议按固定策略落地，而且模型固定为 `Qwen/Qwen3-Reranker-4B`，而不是仅写成可选组件。

### 7.1 适用场景

以下场景必须走 rerank：

- 博客内容问答
- 角色设定问答
- 上传文档问答
- 混合问题：既依赖知识又依赖用户偏好

### 7.2 可跳过场景

以下场景可跳过或弱化 rerank：

- 纯闲聊
- 仅凭最近几轮上下文就能回答的问题
- 明确不需要检索的短指令

### 7.3 排序维度

rerank 至少考虑四个维度：

1. 与当前问题的语义相关性
2. 文档片段是否回答了“真正的问题”
3. 信息可信度
4. 记忆/设定对当前 persona 的优先级

### 7.4 工程要求

- rerank 分数单独记录，方便回放分析
- 保留“召回分数 + rerank 分数”，用于观察误召回
- 当 rerank 后分数整体过低时，回答应降级为“不确定”

---

## 8. LangGraph 节点调整

建议主图更新为：

1. `InputGuardNode`
2. `IntentRouterNode`
3. `PersonaLoadNode`
4. `ShortTermContextNode`
5. `QueryRewriteNode`
6. `PublicRecallNode`
7. `UserMemoryRecallNode`
8. `RerankNode`
9. `ContextAssemblerNode`
10. `ResponseGeneratorNode`
11. `SafetyReviewNode`
12. `MemoryWriteBackNode`
13. `OutputNode`

### 8.1 关键变化

原来“检索后直接组装上下文”的做法，改成：

`Recall -> Rerank -> Assemble`

`RerankNode` 成为主流程固定节点，不再放在后续优化阶段。

---

## 9. 用户记忆设计

### 9.1 记忆类型

建议保留四类：

- 用户偏好记忆
- 用户事实记忆
- 跨会话摘要记忆
- 互动策略记忆

### 9.2 写回策略

不是每轮都写长期记忆，只写：

- 明确长期偏好
- 稳定事实
- 重复出现的信息
- 会显著改善后续回答的信息

### 9.3 检索方式

用户记忆也走：

1. metadata filter
2. vector recall
3. rerank

不要把私有记忆单独绕开 rerank，否则公共知识和私有记忆会难以统一排序。

---

## 10. 回答生成约束

### 10.1 角色与事实分离

- “怎么说”由角色卡控制
- “说什么”由知识库、记忆、当前上下文控制

### 10.2 证据不足时的行为

如果 `Qdrant recall + rerank` 后仍无足够证据：

- 明确说明不确定
- 引导用户补充文章标题、关键词或上传文件
- 不要硬编细节

### 10.3 回答模式

建议默认使用“混合回答”：

1. 先给事实结论
2. 再用角色语气表达
3. 必要时引用来源标题或片段出处

---

## 11. MVP 落地建议

如果现在要先做第一版，建议范围固定为：

1. 单 persona
2. 单主模型：`deepseek-v4-flash`
3. 单 embedding 模型：`Qwen/Qwen3-Embedding-4B`
4. 单向量库：`Qdrant`
5. 固定 rerank 节点
6. 公共知识库 + 用户私有记忆双路检索
7. 文件转 Markdown 入库

第一版不要先做：

- 多 persona 动态切换
- 复杂多工具生态
- 过长 ReAct 推理链
- 不可控的自动长期记忆写入

---

## 12. 最终结论

当前目录下已有内容适合继续作为“架构文档项目”推进，因此最终实现方向应明确固定为：

- 对话生成：`deepseek-v4-flash`
- 向量化：`Qwen/Qwen3-Embedding-4B`
- 重排：`Qwen/Qwen3-Reranker-4B`
- 检索存储：`Qdrant`
- 检索增强：`rerank` 作为默认主链路节点

最终检索主流程应以这条链路为准：

`Query Rewrite -> Qdrant Recall -> Rerank -> Context Assembly -> DeepSeek Answer`

这比单纯“embedding 检索后直接拼上下文”更适合博客问答、角色设定问答和用户记忆混合问答。
