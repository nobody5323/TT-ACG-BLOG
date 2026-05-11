from __future__ import annotations

from collections.abc import Iterator

import pytest
from fastapi.testclient import TestClient

from app.api import deps
from app.core.config import Settings
from app.memory.service import MemoryService
from app.main import app


class StubWorkflow:
    def __init__(self, result: dict | None = None, error: Exception | None = None) -> None:
        self.result = result or {
            "final_response": "stub answer",
            "citations": ["Doc A"],
            "used_rerank": True,
        }
        self.error = error

    def invoke(self, _: dict) -> dict:
        if self.error is not None:
            raise self.error
        return self.result


class StubQdrantStore:
    def __init__(self, created: list[str] | None = None, error: Exception | None = None) -> None:
        self.created = created or ["blog_knowledge", "user_memory"]
        self.error = error

    def ensure_collections(self) -> list[str]:
        if self.error is not None:
            raise self.error
        return self.created


class StubBlogSyncService:
    def __init__(
        self,
        result: str = "synced 2 article(s), removed 0 article(s)",
        error: Exception | None = None,
    ) -> None:
        self.result = result
        self.error = error

    def sync_once(self) -> str:
        if self.error is not None:
            raise self.error
        return self.result


class StubMemoryService:
    def __init__(self, memories: list[dict] | None = None, should_write: bool = True) -> None:
        self.memories = memories or []
        self.should_write = should_write
        self.writes: list[dict] = []

    def retrieve_memories(self, *, query: str, user_id: str, persona_id: str) -> list[dict]:
        return self.memories

    def write_memory(self, *, user_id: str, persona_id: str, user_message: str):
        self.writes.append(
            {
                "user_id": user_id,
                "persona_id": persona_id,
                "user_message": user_message,
            }
        )
        if not self.should_write:
            return None
        return {
            "memory_id": "memory-1",
            "content": user_message,
        }


class StubUploadIngestionService:
    def __init__(self, error: Exception | None = None) -> None:
        self.error = error

    def ingest_bytes(self, *, filename: str, content: bytes, content_type: str | None) -> dict:
        if self.error is not None:
            raise self.error
        return {
            "source_id": "upload-1",
            "chunks": 2,
        }


@pytest.fixture()
def client(tmp_path) -> Iterator[TestClient]:
    settings = Settings(
        _env_file=None,
        APP_NAME="test-app",
        APP_ENV="test",
        CHAT_LOG_PATH=str(tmp_path / "chat-history.jsonl"),
        API_AUTH_TOKEN="test-token",
    )

    app.dependency_overrides[deps.get_app_settings] = lambda: settings
    app.dependency_overrides[deps.get_workflow] = lambda: StubWorkflow()
    app.dependency_overrides[deps.get_qdrant_store] = lambda: StubQdrantStore()
    app.dependency_overrides[deps.get_blog_sync_service] = lambda: StubBlogSyncService()
    app.dependency_overrides[deps.get_memory_service] = lambda: StubMemoryService()
    app.dependency_overrides[deps.get_upload_ingestion_service] = lambda: StubUploadIngestionService()

    with TestClient(app) as test_client:
        yield test_client

    app.dependency_overrides.clear()
