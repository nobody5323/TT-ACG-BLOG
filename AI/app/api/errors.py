from __future__ import annotations

import logging

import httpx
from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from qdrant_client.http.exceptions import ResponseHandlingException
from sqlalchemy.exc import SQLAlchemyError
from starlette.exceptions import HTTPException as StarletteHTTPException

from app.models.schemas import ErrorDetail, ErrorResponse
from app.models.vector_service import ProviderConfigError


logger = logging.getLogger(__name__)


class ApiError(Exception):
    def __init__(self, status_code: int, code: str, message: str) -> None:
        super().__init__(message)
        self.status_code = status_code
        self.code = code
        self.message = message


DEFAULT_ERROR_RESPONSES = {
    401: {"model": ErrorResponse, "description": "Authentication failed"},
    422: {"model": ErrorResponse, "description": "Request validation failed"},
    500: {"model": ErrorResponse, "description": "Internal server error"},
    502: {"model": ErrorResponse, "description": "Upstream service error"},
    503: {"model": ErrorResponse, "description": "Service unavailable"},
}


def register_exception_handlers(app: FastAPI) -> None:
    @app.exception_handler(ApiError)
    async def handle_api_error(_: Request, exc: ApiError) -> JSONResponse:
        return _json_error(exc.status_code, exc.code, exc.message)

    @app.exception_handler(RequestValidationError)
    async def handle_validation_error(_: Request, exc: RequestValidationError) -> JSONResponse:
        logger.info("Request validation failed: %s", exc.errors())
        return _json_error(422, "invalid_request", "Request validation failed")

    @app.exception_handler(StarletteHTTPException)
    async def handle_http_error(_: Request, exc: StarletteHTTPException) -> JSONResponse:
        if exc.status_code == 404:
            return _json_error(404, "not_found", "Resource not found")
        if exc.status_code == 405:
            return _json_error(405, "method_not_allowed", "Method not allowed")
        return _json_error(exc.status_code, "http_error", str(exc.detail))

    @app.exception_handler(Exception)
    async def handle_unexpected_error(_: Request, exc: Exception) -> JSONResponse:
        logger.exception("Unhandled application error")
        return _json_error(500, "internal_error", "An unexpected internal error occurred")


def translate_service_exception(
    exc: Exception,
    *,
    default_code: str,
    default_message: str,
) -> ApiError:
    if isinstance(exc, ApiError):
        return exc
    if isinstance(exc, ProviderConfigError):
        return ApiError(503, "provider_config_error", str(exc))
    if isinstance(exc, httpx.HTTPStatusError):
        return ApiError(
            502,
            "upstream_http_error",
            f"Upstream service returned HTTP {exc.response.status_code}",
        )
    if isinstance(exc, httpx.HTTPError):
        return ApiError(503, "upstream_connection_error", "Failed to reach an upstream service")
    if isinstance(exc, ResponseHandlingException):
        return ApiError(503, "vector_store_error", "Vector store operation failed")
    if isinstance(exc, SQLAlchemyError):
        return ApiError(503, "database_error", "Database operation failed")
    return ApiError(500, default_code, default_message)


def _json_error(status_code: int, code: str, message: str) -> JSONResponse:
    payload = ErrorResponse(error=ErrorDetail(code=code, message=message))
    return JSONResponse(status_code=status_code, content=payload.model_dump())
