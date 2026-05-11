from __future__ import annotations

from uuid import NAMESPACE_URL, uuid5

from qdrant_client import QdrantClient
from qdrant_client.http.models import (
    Distance,
    FieldCondition,
    Filter,
    MatchValue,
    PayloadSchemaType,
    PointStruct,
    VectorParams,
)

from app.core.config import Settings


class QdrantStore:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        self.client = QdrantClient(url=settings.qdrant_url, api_key=settings.qdrant_api_key)

    def ensure_collections(self) -> list[str]:
        created: list[str] = []
        for collection_name in (
            self.settings.qdrant_collection_blog,
            self.settings.qdrant_collection_memory,
        ):
            if not self.client.collection_exists(collection_name):
                self.client.create_collection(
                    collection_name=collection_name,
                    vectors_config=VectorParams(size=self.settings.vector_size, distance=Distance.COSINE),
                )
                created.append(collection_name)
        self.client.create_payload_index(
            collection_name=self.settings.qdrant_collection_blog,
            field_name="source_id",
            field_schema=PayloadSchemaType.KEYWORD,
        )
        self.client.create_payload_index(
            collection_name=self.settings.qdrant_collection_blog,
            field_name="owner_scope",
            field_schema=PayloadSchemaType.KEYWORD,
        )
        self.client.create_payload_index(
            collection_name=self.settings.qdrant_collection_memory,
            field_name="user_id",
            field_schema=PayloadSchemaType.KEYWORD,
        )
        self.client.create_payload_index(
            collection_name=self.settings.qdrant_collection_memory,
            field_name="persona_id",
            field_schema=PayloadSchemaType.KEYWORD,
        )
        return created

    def replace_article_chunks(self, source_id: str, points: list[PointStruct]) -> None:
        source_filter = Filter(
            must=[
                FieldCondition(
                    key="source_id",
                    match=MatchValue(value=source_id),
                )
            ]
        )
        self.client.delete(
            collection_name=self.settings.qdrant_collection_blog,
            points_selector=source_filter,
        )
        if points:
            self.client.upsert(collection_name=self.settings.qdrant_collection_blog, points=points)

    def delete_article_chunks(self, source_id: str) -> None:
        source_filter = Filter(
            must=[
                FieldCondition(
                    key="source_id",
                    match=MatchValue(value=source_id),
                )
            ]
        )
        self.client.delete(
            collection_name=self.settings.qdrant_collection_blog,
            points_selector=source_filter,
        )

    def search_blog(self, vector: list[float], limit: int, owner_scope: str = "public") -> list[dict]:
        query_filter = Filter(
            must=[
                FieldCondition(
                    key="owner_scope",
                    match=MatchValue(value=owner_scope),
                )
            ]
        )
        response = self.client.query_points(
            collection_name=self.settings.qdrant_collection_blog,
            query=vector,
            limit=limit,
            query_filter=query_filter,
            with_payload=True,
            with_vectors=False,
        )
        results = response.points
        return [
            {
                "id": str(item.id),
                "score": item.score,
                "payload": item.payload,
            }
            for item in results
        ]

    def upsert_memory(
        self,
        *,
        memory_id: str,
        vector: list[float],
        payload: dict,
    ) -> None:
        point = PointStruct(
            id=str(uuid5(NAMESPACE_URL, memory_id)),
            vector=vector,
            payload=payload,
        )
        self.client.upsert(collection_name=self.settings.qdrant_collection_memory, points=[point])

    def search_memory(
        self,
        *,
        vector: list[float],
        user_id: str,
        persona_id: str,
        limit: int,
    ) -> list[dict]:
        query_filter = Filter(
            must=[
                FieldCondition(key="user_id", match=MatchValue(value=user_id)),
                FieldCondition(key="persona_id", match=MatchValue(value=persona_id)),
            ]
        )
        response = self.client.query_points(
            collection_name=self.settings.qdrant_collection_memory,
            query=vector,
            limit=limit,
            query_filter=query_filter,
            with_payload=True,
            with_vectors=False,
        )
        return [
            {
                "id": str(item.id),
                "score": item.score,
                "payload": item.payload,
            }
            for item in response.points
        ]
