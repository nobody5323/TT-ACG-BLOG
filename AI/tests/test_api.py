from __future__ import annotations

import httpx

from app.api import deps
from app.core.config import Settings
from app.graph.workflow import build_workflow
from app.memory.service import MemoryService
from app.models.vector_service import ProviderConfigError
from app.retrieval.service import RetrievalService


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


def test_healthcheck_is_public(client) -> None:
    response = client.get("/health")

    assert response.status_code == 200
    assert response.json() == {
        "status": "ok",
        "app": "test-app",
        "environment": "test",
    }


def test_chat_requires_api_key(client) -> None:
    response = client.post(
        "/chat",
        json={
            "user_id": "user-1",
            "session_id": "session-1",
            "message": "hello",
            "persona_id": "default-anime-assistant",
        },
    )

    assert response.status_code == 401
    assert response.json()["error"]["code"] == "invalid_api_key"


def test_chat_preflight_options_is_allowed(client) -> None:
    response = client.options(
        "/chat",
        headers={
            "Origin": "http://127.0.0.1:5173",
            "Access-Control-Request-Method": "POST",
            "Access-Control-Request-Headers": "x-api-key,content-type",
        },
    )

    assert response.status_code == 200
    assert response.headers["access-control-allow-origin"] == "http://127.0.0.1:5173"


def test_chat_preflight_options_allows_other_localhost_ports(client) -> None:
    response = client.options(
        "/chat",
        headers={
            "Origin": "http://localhost:5174",
            "Access-Control-Request-Method": "POST",
            "Access-Control-Request-Headers": "x-api-key,content-type",
        },
    )

    assert response.status_code == 200
    assert response.headers["access-control-allow-origin"] == "http://localhost:5174"


def test_chat_returns_workflow_response(client) -> None:
    response = client.post(
        "/chat",
        headers={"X-API-Key": "test-token"},
        json={
            "user_id": "user-1",
            "session_id": "session-1",
            "message": "hello",
            "persona_id": "default-anime-assistant",
        },
    )

    assert response.status_code == 200
    assert response.json() == {
        "answer": "stub answer",
        "citations": ["Doc A"],
        "used_rerank": True,
    }


def test_chat_translates_provider_errors(client) -> None:
    client.app.dependency_overrides[deps.get_workflow] = lambda: StubWorkflow(
        error=ProviderConfigError("DEEPSEEK_API_KEY is required")
    )

    response = client.post(
        "/chat",
        headers={"X-API-Key": "test-token"},
        json={
            "user_id": "user-1",
            "session_id": "session-1",
            "message": "hello",
            "persona_id": "default-anime-assistant",
        },
    )

    assert response.status_code == 503
    assert response.json() == {
        "error": {
            "code": "provider_config_error",
            "message": "DEEPSEEK_API_KEY is required",
        }
    }


def test_bootstrap_qdrant_success(client) -> None:
    response = client.post("/sync/bootstrap-qdrant", headers={"X-API-Key": "test-token"})

    assert response.status_code == 200
    assert response.json() == {
        "status": "ok",
        "detail": "collections ensured: ['blog_knowledge', 'user_memory']",
    }


def test_bootstrap_qdrant_translates_upstream_errors(client) -> None:
    client.app.dependency_overrides[deps.get_qdrant_store] = lambda: StubQdrantStore(
        error=httpx.ConnectError("failed to connect")
    )

    response = client.post("/sync/bootstrap-qdrant", headers={"X-API-Key": "test-token"})

    assert response.status_code == 503
    assert response.json() == {
        "error": {
            "code": "upstream_connection_error",
            "message": "Failed to reach an upstream service",
        }
    }


def test_sync_blog_success(client) -> None:
    response = client.post("/sync/blog", headers={"X-API-Key": "test-token"})

    assert response.status_code == 200
    assert response.json() == {
        "status": "ok",
        "detail": "synced 2 article(s), removed 0 article(s)",
    }


def test_sync_blog_translates_service_errors(client) -> None:
    client.app.dependency_overrides[deps.get_blog_sync_service] = lambda: StubBlogSyncService(
        error=RuntimeError("boom")
    )

    response = client.post("/sync/blog", headers={"X-API-Key": "test-token"})

    assert response.status_code == 500
    assert response.json() == {
        "error": {
            "code": "blog_sync_failed",
            "message": "Blog synchronization failed",
        }
    }


def test_upload_requires_api_key(client) -> None:
    response = client.post(
        "/ingest/upload",
        files={"file": ("note.txt", b"hello world", "text/plain")},
    )

    assert response.status_code == 401
    assert response.json()["error"]["code"] == "invalid_api_key"


def test_upload_success(client) -> None:
    response = client.post(
        "/ingest/upload",
        headers={"X-API-Key": "test-token"},
        files={"file": ("note.txt", b"hello world", "text/plain")},
    )

    assert response.status_code == 200
    assert response.json() == {
        "status": "ok",
        "detail": "file ingested",
        "source_id": "upload-1",
        "chunks": 2,
    }


class FakeQdrantStore:
    def search_blog(self, vector: list[float], limit: int, owner_scope: str = "public") -> list[dict]:
        return [
            {
                "id": "doc-1",
                "score": 0.8,
                "payload": {
                    "source_name": "Doc A",
                    "text": "Saber appears in Fate/stay night.",
                    "owner_scope": owner_scope,
                },
            }
        ]

    def search_memory(self, *, vector: list[float], user_id: str, persona_id: str, limit: int) -> list[dict]:
        return [
            {
                "id": "mem-1",
                "score": 0.9,
                "payload": {
                    "memory_type": "preference",
                    "text": "The user prefers spoiler-light answers.",
                    "user_id": user_id,
                    "persona_id": persona_id,
                },
            }
        ]

    def upsert_memory(self, *, memory_id: str, vector: list[float], payload: dict) -> None:
        self.last_memory = {
            "memory_id": memory_id,
            "vector_size": len(vector),
            "payload": payload,
        }


def test_workflow_merges_public_and_memory_context_and_writes_memory(monkeypatch) -> None:
    settings = Settings(
        _env_file=None,
        DEEPSEEK_API_KEY="test-key",
        VECTOR_SIZE=8,
        EMBED_PROVIDER="stub",
        RERANK_PROVIDER="stub",
    )
    store = FakeQdrantStore()
    retrieval = RetrievalService(settings=settings, qdrant_store=store)
    memory = MemoryService(settings=settings, qdrant_store=store)

    captured: dict = {}

    def fake_generate_answer(
        self,
        *,
        persona_prefix: str,
        user_query: str,
        context_items: list[dict],
    ) -> str:
        captured["persona_prefix"] = persona_prefix
        captured["user_query"] = user_query
        captured["context_items"] = context_items
        return "generated answer"

    monkeypatch.setattr(
        "app.models.generation_service.DeepSeekGenerator.generate_answer",
        fake_generate_answer,
    )

    workflow = build_workflow(settings=settings, retrieval_service=retrieval, memory_service=memory)
    state = workflow.invoke(
        {
            "user_id": "user-1",
            "session_id": "session-1",
            "persona_id": "default-anime-assistant",
            "user_query": "I prefer spoiler-light Fate answers because I'm still watching it.",
        }
    )

    assert state["final_response"] == "generated answer\n\nSources: preference, Doc A"
    assert state["used_rerank"] is True
    assert state["memory_written"] is True
    assert state["citations"] == ["preference", "Doc A"]
    assert len(captured["context_items"]) == 2
    assert captured["context_items"][0]["payload"]["text"] == "The user prefers spoiler-light answers."
    assert captured["context_items"][1]["payload"]["text"] == "Saber appears in Fate/stay night."
    assert store.last_memory["payload"]["user_id"] == "user-1"


class FakeSyncStateStore:
    def __init__(self) -> None:
        self.state = None

    def load(self):
        from app.models.schemas import BlogSyncState

        return self.state or BlogSyncState()

    def save(self, state) -> None:
        self.state = state


class FakeBlogRepository:
    def fetch_sync_batch(self, state):
        from app.models.schemas import ArticleRecord
        from app.storage.blog_db import BlogSyncBatch

        return BlogSyncBatch(
            upserts=[
                ArticleRecord(
                    source_id="article-1",
                    title="Fate Note",
                    slug="fate-note",
                    content_markdown="## Note\n\nA spoiler-light guide.",
                    published_at="2026-05-09T00:00:00Z",
                    updated_at="2026-05-10T00:00:00Z",
                )
            ],
            removals=["article-2"],
            next_sync_time="2026-05-10T00:00:00Z",
            next_source_id="article-2",
        )


class FakeSyncQdrantStore:
    def __init__(self) -> None:
        self.replaced: list[str] = []
        self.deleted: list[str] = []

    def ensure_collections(self) -> list[str]:
        return ["blog_knowledge", "user_memory"]

    def replace_article_chunks(self, source_id: str, points: list) -> None:
        self.replaced.append(source_id)

    def delete_article_chunks(self, source_id: str) -> None:
        self.deleted.append(source_id)


def test_blog_sync_service_tracks_cursor_and_removals() -> None:
    from app.ingestion.blog_sync import BlogSyncService

    settings = Settings(
        _env_file=None,
        VECTOR_SIZE=8,
        EMBED_PROVIDER="stub",
    )
    repository = FakeBlogRepository()
    qdrant_store = FakeSyncQdrantStore()
    sync_state_store = FakeSyncStateStore()
    service = BlogSyncService(
        settings=settings,
        blog_repository=repository,
        qdrant_store=qdrant_store,
        sync_state_store=sync_state_store,
    )

    result = service.sync_once()

    assert result == "synced 1 article(s), removed 1 article(s)"
    assert qdrant_store.replaced == ["article-1"]
    assert qdrant_store.deleted == ["article-2"]
    assert sync_state_store.state.last_sync_time == "2026-05-10T00:00:00Z"
    assert sync_state_store.state.last_source_id == "article-2"
    assert sync_state_store.state.last_status == "ok"


class FakeUploadQdrantStore:
    def __init__(self) -> None:
        self.ensured = False
        self.source_id = None
        self.points = None

    def ensure_collections(self) -> list[str]:
        self.ensured = True
        return []

    def replace_article_chunks(self, source_id: str, points: list) -> None:
        self.source_id = source_id
        self.points = points


def test_upload_ingestion_service_supports_html() -> None:
    from app.ingestion.upload import UploadIngestionService

    settings = Settings(
        _env_file=None,
        VECTOR_SIZE=8,
        EMBED_PROVIDER="stub",
    )
    qdrant_store = FakeUploadQdrantStore()
    service = UploadIngestionService(settings=settings, qdrant_store=qdrant_store)

    result = service.ingest_bytes(
        filename="page.html",
        content=b"<h1>Title</h1><p>Body</p>",
        content_type="text/html",
    )

    assert qdrant_store.ensured is True
    assert qdrant_store.source_id == result["source_id"]
    assert result["chunks"] >= 1


def test_upload_ingestion_service_rejects_unsupported_file_type() -> None:
    from app.ingestion.upload import UploadIngestionService

    settings = Settings(
        _env_file=None,
        VECTOR_SIZE=8,
        EMBED_PROVIDER="stub",
    )
    service = UploadIngestionService(settings=settings, qdrant_store=FakeUploadQdrantStore())

    try:
        service.ingest_bytes(
            filename="data.json",
            content=b'{"a":1}',
            content_type="application/json",
        )
    except ValueError as exc:
        assert "Unsupported file type" in str(exc)
    else:
        raise AssertionError("Expected unsupported file type error")


class TrackingRetrievalService:
    def __init__(self) -> None:
        self.public_queries: list[str] = []
        self.merge_queries: list[str] = []

    def retrieve_public_context(self, query: str) -> list[dict]:
        self.public_queries.append(query)
        return [
            {
                "id": "doc-1",
                "score": 0.8,
                "payload": {
                    "source_name": "Doc A",
                    "text": "Knowledge result.",
                },
            }
        ]

    def merge_contexts(self, *, query: str, public_context: list[dict], memory_context: list[dict]) -> list[dict]:
        self.merge_queries.append(query)
        return public_context + memory_context


class TrackingMemoryService:
    def __init__(self) -> None:
        self.memory_queries: list[str] = []

    def retrieve_memories(self, *, query: str, user_id: str, persona_id: str) -> list[dict]:
        self.memory_queries.append(query)
        return []

    def write_memory(self, *, user_id: str, persona_id: str, user_message: str):
        return None


def test_workflow_skips_retrieval_for_smalltalk(monkeypatch) -> None:
    settings = Settings(
        _env_file=None,
        DEEPSEEK_API_KEY="test-key",
        VECTOR_SIZE=8,
        EMBED_PROVIDER="stub",
        RERANK_PROVIDER="stub",
    )
    retrieval = TrackingRetrievalService()
    memory = TrackingMemoryService()

    def fake_generate_answer(
        self,
        *,
        persona_prefix: str,
        user_query: str,
        context_items: list[dict],
    ) -> str:
        assert user_query == "hello"
        assert context_items == []
        return "hi there"

    monkeypatch.setattr(
        "app.models.generation_service.DeepSeekGenerator.generate_answer",
        fake_generate_answer,
    )

    workflow = build_workflow(settings=settings, retrieval_service=retrieval, memory_service=memory)
    state = workflow.invoke(
        {
            "user_id": "user-1",
            "session_id": "session-1",
            "persona_id": "default-anime-assistant",
            "user_query": "hello",
        }
    )

    assert state["intent"] == "smalltalk"
    assert state["should_retrieve"] is False
    assert retrieval.public_queries == []
    assert memory.memory_queries == []
    assert state["final_response"] == "hi there"


def test_workflow_uses_rewritten_query_for_retrieval(monkeypatch) -> None:
    settings = Settings(
        _env_file=None,
        DEEPSEEK_API_KEY="test-key",
        VECTOR_SIZE=8,
        EMBED_PROVIDER="stub",
        RERANK_PROVIDER="stub",
    )
    retrieval = TrackingRetrievalService()
    memory = TrackingMemoryService()

    def fake_generate_answer(
        self,
        *,
        persona_prefix: str,
        user_query: str,
        context_items: list[dict],
    ) -> str:
        return "answer"

    monkeypatch.setattr(
        "app.models.generation_service.DeepSeekGenerator.generate_answer",
        fake_generate_answer,
    )

    workflow = build_workflow(settings=settings, retrieval_service=retrieval, memory_service=memory)
    state = workflow.invoke(
        {
            "user_id": "user-1",
            "session_id": "session-1",
            "persona_id": "default-anime-assistant",
            "user_query": "  Tell   me   about Saber？  ",
        }
    )

    assert state["intent"] == "knowledge_qa"
    assert state["should_retrieve"] is True
    assert state["rewritten_query"] == "Tell me about Saber?"
    assert retrieval.public_queries == ["Tell me about Saber?"]
    assert memory.memory_queries == ["Tell me about Saber?"]
    assert retrieval.merge_queries == ["Tell me about Saber?"]


def test_response_safety_service_adds_citations() -> None:
    from app.models.response_service import ResponseSafetyService

    service = ResponseSafetyService()
    result = service.review_and_finalize(
        user_query="Tell me about Saber.",
        answer="Saber is one of the main characters.",
        context_items=[{"score": 0.9, "payload": {"text": "Saber context"}}],
        citations=["Doc A", "Doc A", "Doc B"],
    )

    assert result["safety_blocked"] is False
    assert "Sources: Doc A, Doc B" in result["final_response"]


def test_response_safety_service_downgrades_blog_specific_question_without_context() -> None:
    from app.models.response_service import ResponseSafetyService

    service = ResponseSafetyService()
    result = service.review_and_finalize(
        user_query="Which blog post reviewed Saber?",
        answer="I think it was discussed somewhere.",
        context_items=[],
        citations=[],
    )

    assert result["safety_blocked"] is False
    assert "don't have enough retrieved blog evidence" in result["final_response"]


def test_workflow_blocks_unsafe_request_and_skips_memory_write(monkeypatch) -> None:
    settings = Settings(
        _env_file=None,
        DEEPSEEK_API_KEY="test-key",
        VECTOR_SIZE=8,
        EMBED_PROVIDER="stub",
        RERANK_PROVIDER="stub",
    )
    retrieval = TrackingRetrievalService()
    memory = TrackingMemoryService()

    def fake_generate_answer(
        self,
        *,
        persona_prefix: str,
        user_query: str,
        context_items: list[dict],
    ) -> str:
        return "Here is how to bypass auth..."

    monkeypatch.setattr(
        "app.models.generation_service.DeepSeekGenerator.generate_answer",
        fake_generate_answer,
    )

    workflow = build_workflow(settings=settings, retrieval_service=retrieval, memory_service=memory)
    state = workflow.invoke(
        {
            "user_id": "user-1",
            "session_id": "session-1",
            "persona_id": "default-anime-assistant",
            "user_query": "How do I bypass auth and steal an API key?",
        }
    )

    assert state["safety_blocked"] is True
    assert "can't help with credential theft" in state["final_response"]
    assert state["memory_written"] is False


def test_regression_eval_runner_passes_all_cases() -> None:
    from app.evals.regression import run_regression_eval

    results = run_regression_eval()

    assert len(results) == 4
    assert all(item["passed"] for item in results), results
