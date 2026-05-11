from __future__ import annotations

import hashlib

from app.models.schemas import ChunkRecord


def chunk_markdown(document: str, source_id: str, source_name: str, chunk_size: int = 800) -> list[ChunkRecord]:
    paragraphs = [part.strip() for part in document.split("\n\n") if part.strip()]
    chunks: list[ChunkRecord] = []
    buffer = ""
    chunk_index = 0

    for paragraph in paragraphs:
        candidate = f"{buffer}\n\n{paragraph}".strip() if buffer else paragraph
        if len(candidate) <= chunk_size:
            buffer = candidate
            continue
        if buffer:
            chunks.append(_make_chunk(buffer, source_id, source_name, chunk_index))
            chunk_index += 1
        buffer = paragraph

    if buffer:
        chunks.append(_make_chunk(buffer, source_id, source_name, chunk_index))

    return chunks


def _make_chunk(text: str, source_id: str, source_name: str, chunk_index: int) -> ChunkRecord:
    digest = hashlib.sha1(f"{source_id}:{chunk_index}:{text}".encode("utf-8")).hexdigest()[:16]
    return ChunkRecord(
        chunk_id=f"{source_id}-{digest}",
        chunk_index=chunk_index,
        text=text,
        section_path=source_name,
        payload={},
    )

