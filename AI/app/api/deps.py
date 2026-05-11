from typing import Annotated

from fastapi import Depends
from fastapi.security import APIKeyHeader

from app.api.errors import ApiError
from app.core.config import Settings, get_settings
from app.graph.workflow import build_workflow
from app.ingestion.blog_sync import BlogSyncService
from app.ingestion.upload import UploadIngestionService
from app.memory.service import MemoryService
from app.retrieval.service import RetrievalService
from app.storage.blog_db import BlogRepository
from app.storage.qdrant import QdrantStore
from app.storage.sync_state import SyncStateStore


api_key_header = APIKeyHeader(name="X-API-Key", auto_error=False)


def get_app_settings() -> Settings:
    return get_settings()


def require_api_key(
    settings: Annotated[Settings, Depends(get_app_settings)],
    api_key: Annotated[str | None, Depends(api_key_header)],
) -> None:
    expected = settings.api_auth_token
    if not expected:
        raise ApiError(503, "auth_not_configured", "API authentication is not configured")
    if api_key != expected:
        raise ApiError(401, "invalid_api_key", "Invalid or missing API key")


def get_qdrant_store() -> QdrantStore:
    return QdrantStore(get_settings())


def get_blog_repository() -> BlogRepository:
    return BlogRepository(get_settings())


def get_sync_state_store() -> SyncStateStore:
    return SyncStateStore(get_settings())


def get_retrieval_service() -> RetrievalService:
    settings = get_settings()
    return RetrievalService(settings=settings, qdrant_store=get_qdrant_store())


def get_memory_service() -> MemoryService:
    settings = get_settings()
    return MemoryService(settings=settings, qdrant_store=get_qdrant_store())


def get_blog_sync_service() -> BlogSyncService:
    settings = get_settings()
    return BlogSyncService(
        settings=settings,
        blog_repository=get_blog_repository(),
        qdrant_store=get_qdrant_store(),
        sync_state_store=get_sync_state_store(),
    )


def get_upload_ingestion_service() -> UploadIngestionService:
    settings = get_settings()
    return UploadIngestionService(settings=settings, qdrant_store=get_qdrant_store())


def get_workflow():
    settings = get_settings()
    retrieval_service = get_retrieval_service()
    memory_service = get_memory_service()
    return build_workflow(
        settings=settings,
        retrieval_service=retrieval_service,
        memory_service=memory_service,
    )
