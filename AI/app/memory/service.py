from __future__ import annotations

from datetime import datetime, timezone
from uuid import NAMESPACE_URL, uuid5

from app.core.config import Settings
from app.models.schemas import MemoryRecord
from app.models.vector_service import Embedder, Reranker
from app.storage.qdrant import QdrantStore


class MemoryService:
    def __init__(self, settings: Settings, qdrant_store: QdrantStore) -> None:
        self.settings = settings
        self.qdrant_store = qdrant_store
        self.embedder = Embedder(settings)
        self.reranker = Reranker(settings)

    def should_write_back(self, message: str) -> bool:
        normalized = " ".join(message.split())
        if len(normalized) < 24:
            return False
        lowered = normalized.lower()
        weak_patterns = (
            "hello",
            "hi ",
            "thanks",
            "thank you",
            "what do you think",
            "can you help",
        )
        return not any(pattern in lowered for pattern in weak_patterns)

    def build_candidate_memory(
        self,
        *,
        user_id: str,
        persona_id: str,
        user_message: str,
    ) -> MemoryRecord:
        timestamp = datetime.now(timezone.utc).isoformat()
        memory_type = self._infer_memory_type(user_message)
        normalized = " ".join(user_message.split())
        memory_id = str(uuid5(NAMESPACE_URL, f"{user_id}:{persona_id}:{memory_type}:{normalized}"))
        return MemoryRecord(
            memory_id=memory_id,
            user_id=user_id,
            persona_id=persona_id,
            memory_type=memory_type,
            content=normalized,
            importance=self._estimate_importance(normalized, memory_type),
            confirmed=memory_type in {"preference", "profile_fact"},
            created_at=timestamp,
            updated_at=timestamp,
        )

    def write_memory(
        self,
        *,
        user_id: str,
        persona_id: str,
        user_message: str,
    ) -> MemoryRecord | None:
        if not self.should_write_back(user_message):
            return None

        record = self.build_candidate_memory(
            user_id=user_id,
            persona_id=persona_id,
            user_message=user_message,
        )
        payload = record.model_dump()
        payload["owner_scope"] = "private"
        payload["text"] = record.content
        self.qdrant_store.upsert_memory(
            memory_id=record.memory_id,
            vector=self.embedder.embed_text(record.content),
            payload=payload,
        )
        return record

    def retrieve_memories(self, *, query: str, user_id: str, persona_id: str) -> list[dict]:
        vector = self.embedder.embed_text(query)
        recalled = self.qdrant_store.search_memory(
            vector=vector,
            user_id=user_id,
            persona_id=persona_id,
            limit=self.settings.default_top_k_memory,
        )
        return self.reranker.rerank(
            query=query,
            candidates=recalled,
            limit=min(self.settings.default_top_k_memory, 4),
        )

    @staticmethod
    def _infer_memory_type(message: str) -> str:
        lowered = message.lower()
        if any(token in lowered for token in ("i like", "i love", "prefer", "favorite")):
            return "preference"
        if any(token in lowered for token in ("i am", "i work", "my name", "i live")):
            return "profile_fact"
        return "conversation_note"

    @staticmethod
    def _estimate_importance(message: str, memory_type: str) -> float:
        base_score = 0.55
        if memory_type != "conversation_note":
            base_score += 0.2
        if len(message) > 80:
            base_score += 0.1
        return min(base_score, 1.0)
