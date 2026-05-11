from __future__ import annotations

from datetime import datetime, timezone
from uuid import NAMESPACE_URL, uuid5

from qdrant_client.http.models import PointStruct

from app.core.config import Settings
from app.ingestion.chunking import chunk_markdown
from app.ingestion.normalizer import article_to_markdown
from app.models.schemas import ArticleRecord
from app.models.vector_service import Embedder
from app.storage.blog_db import BlogRepository
from app.storage.qdrant import QdrantStore
from app.storage.sync_state import SyncStateStore


class BlogSyncService:
    def __init__(
        self,
        settings: Settings,
        blog_repository: BlogRepository,
        qdrant_store: QdrantStore,
        sync_state_store: SyncStateStore,
    ) -> None:
        self.settings = settings
        self.blog_repository = blog_repository
        self.qdrant_store = qdrant_store
        self.sync_state_store = sync_state_store
        self.embedder = Embedder(settings)

    def sync_once(self) -> str:
        self.qdrant_store.ensure_collections()
        state = self.sync_state_store.load()
        batch = self.blog_repository.fetch_sync_batch(state)
        synced = 0
        removed = 0
        for article in batch.upserts:
            self._sync_article(article)
            synced += 1
        for source_id in batch.removals:
            self.qdrant_store.delete_article_chunks(source_id)
            removed += 1

        new_state = state.model_copy(
            update={
                "last_sync_time": batch.next_sync_time,
                "last_source_id": batch.next_source_id,
                "last_success_at": datetime.now(timezone.utc).isoformat(),
                "last_status": "ok",
                "last_error": None,
            }
        )
        self.sync_state_store.save(new_state)
        return f"synced {synced} article(s), removed {removed} article(s)"

    def _sync_article(self, article: ArticleRecord) -> None:
        markdown = article_to_markdown(article)
        chunks = chunk_markdown(markdown, source_id=article.source_id, source_name=article.title)
        points: list[PointStruct] = []
        for chunk in chunks:
            payload = {
                "source_type": "blog_post",
                "source_id": article.source_id,
                "source_name": article.title,
                "slug": article.slug,
                "summary": article.summary or "",
                "tags": article.tags,
                "category": article.category or "",
                "chunk_id": chunk.chunk_id,
                "chunk_index": chunk.chunk_index,
                "section_path": chunk.section_path,
                "text": chunk.text,
                "owner_scope": "public",
                "published_at": article.published_at or "",
                "updated_at": article.updated_at or "",
                "is_approved": True,
            }
            points.append(
                PointStruct(
                    id=str(uuid5(NAMESPACE_URL, chunk.chunk_id)),
                    vector=self.embedder.embed_text(chunk.text),
                    payload=payload,
                )
            )
        self.qdrant_store.replace_article_chunks(article.source_id, points)
