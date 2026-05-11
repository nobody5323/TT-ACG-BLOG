from __future__ import annotations

from pathlib import Path
from uuid import NAMESPACE_URL, uuid5

from qdrant_client.http.models import PointStruct

from app.core.config import Settings
from app.ingestion.chunking import chunk_markdown
from app.models.vector_service import Embedder
from app.storage.qdrant import QdrantStore


class UploadIngestionService:
    def __init__(self, settings: Settings, qdrant_store: QdrantStore) -> None:
        self.settings = settings
        self.qdrant_store = qdrant_store
        self.embedder = Embedder(settings)

    def ingest_bytes(self, *, filename: str, content: bytes, content_type: str | None) -> dict:
        source_id = str(uuid5(NAMESPACE_URL, f"upload:{filename}:{len(content)}"))
        markdown = self._to_markdown(filename=filename, content=content, content_type=content_type)
        chunks = chunk_markdown(markdown, source_id=source_id, source_name=filename)
        points: list[PointStruct] = []
        for chunk in chunks:
            payload = {
                "source_type": "uploaded_file",
                "source_id": source_id,
                "source_name": filename,
                "slug": "",
                "summary": "",
                "tags": ["upload"],
                "category": "upload",
                "chunk_id": chunk.chunk_id,
                "chunk_index": chunk.chunk_index,
                "section_path": chunk.section_path,
                "text": chunk.text,
                "owner_scope": "public",
                "published_at": "",
                "updated_at": "",
                "is_approved": True,
            }
            points.append(
                PointStruct(
                    id=str(uuid5(NAMESPACE_URL, chunk.chunk_id)),
                    vector=self.embedder.embed_text(chunk.text),
                    payload=payload,
                )
            )
        self.qdrant_store.ensure_collections()
        self.qdrant_store.replace_article_chunks(source_id, points)
        return {
            "source_id": source_id,
            "chunks": len(points),
        }

    def _to_markdown(self, *, filename: str, content: bytes, content_type: str | None) -> str:
        suffix = Path(filename).suffix.lower()
        text = content.decode("utf-8")
        if suffix in {".md", ".markdown", ".txt"}:
            return text.strip()
        if suffix == ".html" or content_type == "text/html":
            from markdownify import markdownify as html_to_markdown

            return html_to_markdown(text, heading_style="ATX").strip()
        raise ValueError(f"Unsupported file type: {suffix or content_type or 'unknown'}")
