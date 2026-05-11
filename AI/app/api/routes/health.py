from typing import Annotated

from fastapi import APIRouter, Depends

from app.api.deps import get_app_settings
from app.core.config import Settings


router = APIRouter()


@router.get("/health")
def healthcheck(settings: Annotated[Settings, Depends(get_app_settings)]) -> dict[str, str]:
    return {
        "status": "ok",
        "app": settings.app_name,
        "environment": settings.app_env,
    }
