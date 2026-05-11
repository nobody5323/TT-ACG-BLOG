from markdownify import markdownify as html_to_markdown

from app.models.schemas import ArticleRecord


def article_to_markdown(article: ArticleRecord) -> str:
    body = article.content_markdown or _html_to_markdown(article.content_html or "")
    summary = article.summary or ""
    tags = ", ".join(article.tags)
    category = article.category or ""
    published_at = article.published_at or ""
    return (
        f"# {article.title}\n\n"
        f"URL: /posts/{article.slug}\n"
        f"PublishedAt: {published_at}\n"
        f"Tags: {tags}\n"
        f"Category: {category}\n\n"
        f"## Summary\n\n{summary}\n\n"
        f"## Content\n\n{body.strip()}\n"
    ).strip()


def _html_to_markdown(content_html: str) -> str:
    if not content_html.strip():
        return ""
    return html_to_markdown(content_html, heading_style="ATX")

