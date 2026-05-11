from typing import Annotated

from fastapi import APIRouter, Depends, File, UploadFile

from app.api.deps import get_upload_ingestion_service
from app.api.errors import DEFAULT_ERROR_RESPONSES, translate_service_exception
from app.ingestion.upload import UploadIngestionService
from app.models.schemas import IngestResponse


router = APIRouter()


@router.post("/upload", response_model=IngestResponse, responses=DEFAULT_ERROR_RESPONSES)
async def upload_document(
    file: Annotated[UploadFile, File(...)],
    service: Annotated[UploadIngestionService, Depends(get_upload_ingestion_service)],
) -> IngestResponse:
    try:
        content = await file.read()
        result = service.ingest_bytes(
            filename=file.filename or "upload.txt",
            content=content,
            content_type=file.content_type,
        )
    except Exception as exc:
        raise translate_service_exception(
            exc,
            default_code="upload_ingestion_failed",
            default_message="File ingestion failed",
        ) from exc
    return IngestResponse(
        status="ok",
        detail="file ingested",
        source_id=result["source_id"],
        chunks=result["chunks"],
    )
