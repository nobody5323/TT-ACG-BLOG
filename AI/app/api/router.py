from fastapi import APIRouter, Depends

from app.api.deps import require_api_key
from app.api.routes.chat import router as chat_router
from app.api.routes.health import router as health_router
from app.api.routes.ingest import router as ingest_router
from app.api.routes.sync import router as sync_router


api_router = APIRouter()
api_router.include_router(health_router, tags=["health"])
api_router.include_router(
    chat_router,
    prefix="/chat",
    tags=["chat"],
    dependencies=[Depends(require_api_key)],
)
api_router.include_router(
    sync_router,
    prefix="/sync",
    tags=["sync"],
    dependencies=[Depends(require_api_key)],
)
api_router.include_router(
    ingest_router,
    prefix="/ingest",
    tags=["ingest"],
    dependencies=[Depends(require_api_key)],
)
