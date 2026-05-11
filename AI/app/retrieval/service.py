from __future__ import annotations

from app.core.config import Settings
from app.models.vector_service import Embedder, Reranker
from app.storage.qdrant import QdrantStore


class RetrievalService:
    def __init__(self, settings: Settings, qdrant_store: QdrantStore) -> None:
        self.settings = settings
        self.qdrant_store = qdrant_store
        self.embedder = Embedder(settings)
        self.reranker = Reranker(settings)

    def retrieve_public_context(self, query: str) -> list[dict]:
        vector = self.embedder.embed_text(query)
        recalled = self.qdrant_store.search_blog(vector, limit=self.settings.default_top_k_blog)
        return self.reranker.rerank(
            query=query,
            candidates=recalled,
            limit=self.settings.default_top_n_context,
        )

    def merge_contexts(
        self,
        *,
        query: str,
        public_context: list[dict],
        memory_context: list[dict],
    ) -> list[dict]:
        if not public_context and not memory_context:
            return []

        candidates: list[dict] = []
        for item in public_context:
            candidate = dict(item)
            payload = dict(candidate.get("payload") or {})
            payload.setdefault("candidate_type", "public_doc")
            candidate["payload"] = payload
            candidates.append(candidate)
        for item in memory_context:
            candidate = dict(item)
            payload = dict(candidate.get("payload") or {})
            payload.setdefault("candidate_type", "user_memory")
            candidate["payload"] = payload
            candidates.append(candidate)

        return self.reranker.rerank(
            query=query,
            candidates=candidates,
            limit=self.settings.default_top_n_context,
        )
