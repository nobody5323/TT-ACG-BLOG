from functools import lru_cache
from pathlib import Path

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


def _default_chat_log_path() -> str:
    project_root = Path(__file__).resolve().parents[2]
    return str(project_root / "acg-blog-chat-history.jsonl")


def _default_blog_sync_state_path() -> str:
    project_root = Path(__file__).resolve().parents[2]
    return str(project_root / "blog-sync-state.json")


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    app_name: str = Field(default="acg-blog-ai", alias="APP_NAME")
    app_env: str = Field(default="development", alias="APP_ENV")
    app_host: str = Field(default="0.0.0.0", alias="APP_HOST")
    app_port: int = Field(default=8000, alias="APP_PORT")
    log_level: str = Field(default="INFO", alias="LOG_LEVEL")
    api_auth_token: str | None = Field(default=None, alias="API_AUTH_TOKEN")
    cors_allow_origins: str = Field(
        default="http://127.0.0.1:5173,http://localhost:5173",
        alias="CORS_ALLOW_ORIGINS",
    )
    cors_allow_origin_regex: str = Field(
        default=r"^https?://(127\.0\.0\.1|localhost)(:\d+)?$",
        alias="CORS_ALLOW_ORIGIN_REGEX",
    )

    deepseek_api_key: str | None = Field(default=None, alias="DEEPSEEK_API_KEY")
    deepseek_base_url: str | None = Field(default=None, alias="DEEPSEEK_BASE_URL")
    deepseek_model: str = Field(default="deepseek-v4-flash", alias="DEEPSEEK_MODEL")

    embed_provider: str = Field(default="stub", alias="EMBED_PROVIDER")
    embed_model: str = Field(default="Qwen/Qwen3-Embedding-4B", alias="EMBED_MODEL")
    embed_api_base: str | None = Field(default=None, alias="EMBED_API_BASE")
    embed_api_key: str | None = Field(default=None, alias="EMBED_API_KEY")
    embed_dimensions: int | None = Field(default=None, alias="EMBED_DIMENSIONS")

    rerank_provider: str = Field(default="stub", alias="RERANK_PROVIDER")
    rerank_model: str = Field(default="Qwen/Qwen3-Reranker-4B", alias="RERANK_MODEL")
    rerank_api_base: str | None = Field(default=None, alias="RERANK_API_BASE")
    rerank_api_key: str | None = Field(default=None, alias="RERANK_API_KEY")

    qdrant_url: str = Field(default="http://localhost:6333", alias="QDRANT_URL")
    qdrant_api_key: str | None = Field(default=None, alias="QDRANT_API_KEY")
    qdrant_collection_blog: str = Field(default="blog_knowledge", alias="QDRANT_COLLECTION_BLOG")
    qdrant_collection_memory: str = Field(default="user_memory", alias="QDRANT_COLLECTION_MEMORY")
    vector_size: int = Field(default=2560, alias="VECTOR_SIZE")
    http_timeout_seconds: float = Field(default=30.0, alias="HTTP_TIMEOUT_SECONDS")

    blog_db_url: str | None = Field(default=None, alias="BLOG_DB_URL")
    blog_db_batch_size: int = Field(default=100, alias="BLOG_DB_BATCH_SIZE")
    chat_log_path: str = Field(default_factory=_default_chat_log_path, alias="CHAT_LOG_PATH")
    blog_sync_state_path: str = Field(
        default_factory=_default_blog_sync_state_path,
        alias="BLOG_SYNC_STATE_PATH",
    )

    default_persona_id: str = Field(default="default-anime-assistant", alias="DEFAULT_PERSONA_ID")
    default_top_k_blog: int = Field(default=15, alias="DEFAULT_TOP_K_BLOG")
    default_top_k_memory: int = Field(default=8, alias="DEFAULT_TOP_K_MEMORY")
    default_top_n_context: int = Field(default=6, alias="DEFAULT_TOP_N_CONTEXT")


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    return Settings()
