from __future__ import annotations

from dataclasses import dataclass

from sqlalchemy import create_engine, text

from app.core.config import Settings
from app.models.schemas import ArticleRecord, BlogSyncState


@dataclass
class BlogSyncBatch:
    upserts: list[ArticleRecord]
    removals: list[str]
    next_sync_time: str | None
    next_source_id: str | None


class BlogRepository:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        self.engine = create_engine(settings.blog_db_url) if settings.blog_db_url else None

    def fetch_published_articles(self, limit: int | None = None) -> list[ArticleRecord]:
        if self.engine is None:
            return self._demo_articles()

        sql = text(
            """
            SELECT
              CAST(p.id AS CHAR) AS source_id,
              p.title,
              COALESCE(NULLIF(p.slug, ''), CONCAT('post-', p.id)) AS slug,
              p.summary,
              p.content AS content_markdown,
              NULL AS content_html,
              CAST(p.publish_status AS CHAR) AS status,
              CAST(p.visibility AS CHAR) AS visibility,
              CAST(p.published_time AS CHAR) AS published_at,
              CAST(p.update_time AS CHAR) AS updated_at,
              COALESCE(c.category_name, '') AS category,
              COALESCE(
                GROUP_CONCAT(DISTINCT t.tag_name ORDER BY t.tag_name SEPARATOR '||'),
                ''
              ) AS tags_csv
            FROM content_post p
            LEFT JOIN content_category c ON c.id = p.category_id
            LEFT JOIN content_post_tag pt ON pt.post_id = p.id
            LEFT JOIN content_tag t ON t.id = pt.tag_id
            WHERE p.publish_status = 1
              AND p.deleted = 0
            GROUP BY
              p.id,
              p.title,
              p.slug,
              p.summary,
              p.content,
              p.publish_status,
              p.visibility,
              p.published_time,
              p.update_time,
              c.category_name
            ORDER BY p.update_time DESC, p.id DESC
            LIMIT :limit
            """
        )
        row_limit = limit or self.settings.blog_db_batch_size
        with self.engine.begin() as connection:
            rows = connection.execute(sql, {"limit": row_limit}).mappings().all()

        articles: list[ArticleRecord] = []
        for row in rows:
            data = dict(row)
            tags_csv = data.pop("tags_csv", "") or ""
            data["tags"] = [item for item in tags_csv.split("||") if item]
            articles.append(ArticleRecord(**data))
        return articles

    def fetch_sync_batch(self, state: BlogSyncState) -> BlogSyncBatch:
        if self.engine is None:
            articles = self._demo_articles()
            last_article = articles[-1] if articles else None
            return BlogSyncBatch(
                upserts=articles,
                removals=[],
                next_sync_time=last_article.updated_at if last_article else state.last_sync_time,
                next_source_id=last_article.source_id if last_article else state.last_source_id,
            )

        row_limit = self.settings.blog_db_batch_size
        params = {
            "last_sync_time": state.last_sync_time or "",
            "last_source_id": state.last_source_id or "",
            "limit": row_limit,
        }
        sql = text(
            """
            SELECT
              CAST(p.id AS CHAR) AS source_id,
              p.title,
              COALESCE(NULLIF(p.slug, ''), CONCAT('post-', p.id)) AS slug,
              p.summary,
              p.content AS content_markdown,
              NULL AS content_html,
              CAST(p.publish_status AS CHAR) AS status,
              CAST(p.visibility AS CHAR) AS visibility,
              CAST(p.published_time AS CHAR) AS published_at,
              CAST(p.update_time AS CHAR) AS updated_at,
              COALESCE(c.category_name, '') AS category,
              COALESCE(
                GROUP_CONCAT(DISTINCT t.tag_name ORDER BY t.tag_name SEPARATOR '||'),
                ''
              ) AS tags_csv,
              p.publish_status = 1 AND p.deleted = 0 AND CAST(p.visibility AS CHAR) = 'public' AS is_indexable
            FROM content_post p
            LEFT JOIN content_category c ON c.id = p.category_id
            LEFT JOIN content_post_tag pt ON pt.post_id = p.id
            LEFT JOIN content_tag t ON t.id = pt.tag_id
            WHERE (
                CAST(p.update_time AS CHAR) > :last_sync_time
                OR (
                    CAST(p.update_time AS CHAR) = :last_sync_time
                    AND CAST(p.id AS CHAR) > :last_source_id
                )
            )
            GROUP BY
              p.id,
              p.title,
              p.slug,
              p.summary,
              p.content,
              p.publish_status,
              p.visibility,
              p.published_time,
              p.update_time,
              c.category_name,
              p.deleted
            ORDER BY p.update_time ASC, p.id ASC
            LIMIT :limit
            """
        )
        with self.engine.begin() as connection:
            rows = connection.execute(sql, params).mappings().all()

        upserts: list[ArticleRecord] = []
        removals: list[str] = []
        next_sync_time = state.last_sync_time
        next_source_id = state.last_source_id

        for row in rows:
            data = dict(row)
            is_indexable = bool(data.pop("is_indexable", False))
            tags_csv = data.pop("tags_csv", "") or ""
            data["tags"] = [item for item in tags_csv.split("||") if item]
            article = ArticleRecord(**data)
            next_sync_time = article.updated_at or next_sync_time
            next_source_id = article.source_id
            if is_indexable:
                upserts.append(article)
            else:
                removals.append(article.source_id)

        return BlogSyncBatch(
            upserts=upserts,
            removals=removals,
            next_sync_time=next_sync_time,
            next_source_id=next_source_id,
        )

    def _demo_articles(self) -> list[ArticleRecord]:
        return [
            ArticleRecord(
                source_id="demo-1",
                title="Saber Route Review",
                slug="saber-route-review",
                summary="A short example article used to bootstrap the ingestion pipeline.",
                content_markdown=(
                    "# Saber Route Review\n\n"
                    "This is a seeded article for the scaffold. "
                    "It demonstrates how a post becomes Markdown, chunks, and Qdrant payloads."
                ),
                tags=["demo", "review"],
                category="example",
                published_at="2026-05-07T00:00:00Z",
                updated_at="2026-05-07T00:00:00Z",
            )
        ]
