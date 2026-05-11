from typing import Annotated

from fastapi import APIRouter, Depends

from app.api.deps import get_blog_sync_service, get_qdrant_store
from app.api.errors import DEFAULT_ERROR_RESPONSES, translate_service_exception
from app.ingestion.blog_sync import BlogSyncService
from app.models.schemas import SyncResponse
from app.storage.qdrant import QdrantStore


router = APIRouter()


@router.post("/bootstrap-qdrant", response_model=SyncResponse, responses=DEFAULT_ERROR_RESPONSES)
def bootstrap_qdrant(store: Annotated[QdrantStore, Depends(get_qdrant_store)]) -> SyncResponse:
    try:
        created = store.ensure_collections()
    except Exception as exc:
        raise translate_service_exception(
            exc,
            default_code="bootstrap_qdrant_failed",
            default_message="Failed to ensure Qdrant collections",
        ) from exc
    return SyncResponse(status="ok", detail=f"collections ensured: {created}")


@router.post("/blog", response_model=SyncResponse, responses=DEFAULT_ERROR_RESPONSES)
def sync_blog_articles(service: Annotated[BlogSyncService, Depends(get_blog_sync_service)]) -> SyncResponse:
    try:
        result = service.sync_once()
    except Exception as exc:
        raise translate_service_exception(
            exc,
            default_code="blog_sync_failed",
            default_message="Blog synchronization failed",
        ) from exc
    return SyncResponse(status="ok", detail=result)
