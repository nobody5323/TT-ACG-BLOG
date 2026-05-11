from pydantic import BaseModel, Field


class ChatRequest(BaseModel):
    user_id: str
    session_id: str
    message: str
    persona_id: str = Field(default="default-anime-assistant")


class ChatResponse(BaseModel):
    answer: str
    citations: list[str] = Field(default_factory=list)
    used_rerank: bool = False


class SyncResponse(BaseModel):
    status: str
    detail: str


class IngestResponse(BaseModel):
    status: str
    detail: str
    source_id: str
    chunks: int


class ErrorDetail(BaseModel):
    code: str
    message: str


class ErrorResponse(BaseModel):
    error: ErrorDetail


class ArticleRecord(BaseModel):
    source_id: str
    title: str
    slug: str
    summary: str | None = None
    content_markdown: str | None = None
    content_html: str | None = None
    tags: list[str] = Field(default_factory=list)
    category: str | None = None
    status: str = "published"
    visibility: str = "public"
    published_at: str | None = None
    updated_at: str | None = None


class ChunkRecord(BaseModel):
    chunk_id: str
    chunk_index: int
    text: str
    section_path: str
    payload: dict


class MemoryRecord(BaseModel):
    memory_id: str
    user_id: str
    persona_id: str
    memory_type: str
    content: str
    importance: float = 0.5
    confirmed: bool = False
    created_at: str
    updated_at: str


class BlogSyncState(BaseModel):
    source_name: str = "blog_db"
    last_sync_time: str | None = None
    last_source_id: str | None = None
    last_success_at: str | None = None
    last_status: str = "idle"
    last_error: str | None = None
