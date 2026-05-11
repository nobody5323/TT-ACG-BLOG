from __future__ import annotations

import hashlib
from urllib.parse import urljoin

import httpx

from app.core.config import Settings


class ProviderConfigError(RuntimeError):
    """Raised when a remote model provider is not configured correctly."""


class Embedder:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings

    def embed_text(self, text: str) -> list[float]:
        return self.embed_texts([text])[0]

    def embed_texts(self, texts: list[str]) -> list[list[float]]:
        provider = self.settings.embed_provider.lower()
        if provider == "stub":
            return [self._stub_embed(text) for text in texts]
        if provider == "qwen_cloud":
            return self._embed_openai_compatible(texts, api_key_required=True)
        if provider == "openai_compatible":
            return self._embed_openai_compatible(texts, api_key_required=False)
        if provider == "tei":
            return self._embed_tei(texts)
        raise ProviderConfigError(f"Unsupported embed provider: {self.settings.embed_provider}")

    def _stub_embed(self, text: str) -> list[float]:
        digest = hashlib.sha256(text.encode("utf-8")).digest()
        values = [byte / 255 for byte in digest]
        vector = values * (self.settings.vector_size // len(values) + 1)
        return vector[: self.settings.vector_size]

    def _embed_openai_compatible(self, texts: list[str], api_key_required: bool) -> list[list[float]]:
        base_url = self._require_value(self.settings.embed_api_base, "EMBED_API_BASE")
        api_key = self.settings.embed_api_key
        if api_key_required and not api_key:
            raise ProviderConfigError("EMBED_API_KEY is required for EMBED_PROVIDER=qwen_cloud")

        payload: dict = {
            "model": self.settings.embed_model,
            "input": texts[0] if len(texts) == 1 else texts,
            "encoding_format": "float",
        }
        if self.settings.embed_dimensions:
            payload["dimensions"] = self.settings.embed_dimensions

        headers = self._build_headers(api_key)
        data = self._post_json(self._join_url(base_url, "/embeddings"), payload, headers)
        embeddings = [item["embedding"] for item in data["data"]]
        return embeddings

    def _embed_tei(self, texts: list[str]) -> list[list[float]]:
        base_url = self._require_value(self.settings.embed_api_base, "EMBED_API_BASE")
        payload = {"inputs": texts[0] if len(texts) == 1 else texts}
        data = self._post_json(self._join_url(base_url, "/embed"), payload, headers={})

        # Single TEI input often returns a flat vector; batch input returns a list of vectors.
        if isinstance(data, list) and data and isinstance(data[0], (float, int)):
            return [[float(value) for value in data]]
        if isinstance(data, list):
            return [[float(value) for value in item] for item in data]
        raise RuntimeError("Unexpected TEI embedding response shape")

    def _post_json(self, url: str, payload: dict, headers: dict[str, str]) -> dict | list:
        with httpx.Client(timeout=self.settings.http_timeout_seconds) as client:
            response = client.post(url, json=payload, headers=headers)
            response.raise_for_status()
            return response.json()

    @staticmethod
    def _build_headers(api_key: str | None) -> dict[str, str]:
        headers = {"Content-Type": "application/json"}
        if api_key:
            headers["Authorization"] = f"Bearer {api_key}"
        return headers

    @staticmethod
    def _join_url(base_url: str, path: str) -> str:
        normalized = base_url.rstrip("/") + "/"
        return urljoin(normalized, path.lstrip("/"))

    @staticmethod
    def _require_value(value: str | None, env_name: str) -> str:
        if value:
            return value
        raise ProviderConfigError(f"{env_name} must be set")


class Reranker:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings

    def rerank(self, query: str, candidates: list[dict], limit: int) -> list[dict]:
        if not candidates:
            return []

        provider = self.settings.rerank_provider.lower()
        if provider == "stub":
            ranked = sorted(candidates, key=lambda item: item.get("score", 0), reverse=True)
            return ranked[:limit]
        if provider == "qwen_cloud":
            return self._rerank_qwen_cloud(query, candidates, limit)
        if provider == "openai_compatible":
            return self._rerank_openai_compatible(query, candidates, limit)
        if provider == "tei":
            return self._rerank_tei(query, candidates, limit)
        raise ProviderConfigError(f"Unsupported rerank provider: {self.settings.rerank_provider}")

    def _rerank_qwen_cloud(self, query: str, candidates: list[dict], limit: int) -> list[dict]:
        url = self._rerank_endpoint(self._require_value(self.settings.rerank_api_base, "RERANK_API_BASE"))
        api_key = self.settings.rerank_api_key
        if not api_key:
            raise ProviderConfigError("RERANK_API_KEY is required for RERANK_PROVIDER=qwen_cloud")

        payload = {
            "model": self.settings.rerank_model,
            "query": query,
            "documents": [self._candidate_text(item) for item in candidates],
            "top_n": limit,
        }
        headers = self._build_headers(api_key)
        data = self._post_json(url, payload, headers)
        return self._merge_rerank_results(candidates, data)

    def _rerank_openai_compatible(self, query: str, candidates: list[dict], limit: int) -> list[dict]:
        base_url = self._require_value(self.settings.rerank_api_base, "RERANK_API_BASE")
        payload = {
            "model": self.settings.rerank_model,
            "query": query,
            "documents": [self._candidate_text(item) for item in candidates],
            "top_n": limit,
        }
        data = self._post_json(self._rerank_endpoint(base_url), payload, headers={})
        return self._merge_rerank_results(candidates, data)

    def _rerank_tei(self, query: str, candidates: list[dict], limit: int) -> list[dict]:
        base_url = self._require_value(self.settings.rerank_api_base, "RERANK_API_BASE")
        payload = {
            "query": query,
            "texts": [self._candidate_text(item) for item in candidates],
            "raw_scores": False,
        }
        data = self._post_json(self._join_url(base_url, "/rerank"), payload, headers={})
        ranked = self._merge_rerank_results(candidates, data)
        return ranked[:limit]

    def _merge_rerank_results(self, candidates: list[dict], data: dict | list) -> list[dict]:
        if isinstance(data, list):
            items = data
        else:
            items = data.get("results") or data.get("data") or data.get("output", {}).get("results") or []

        reranked: list[dict] = []
        for item in items:
            index = item.get("index")
            if index is None or index >= len(candidates):
                continue
            candidate = dict(candidates[index])
            candidate["rerank_score"] = item.get("relevance_score", item.get("score", 0.0))
            reranked.append(candidate)

        if reranked:
            reranked.sort(key=lambda item: item.get("rerank_score", 0.0), reverse=True)
            return reranked

        fallback = sorted(candidates, key=lambda item: item.get("score", 0), reverse=True)
        return fallback

    @staticmethod
    def _candidate_text(candidate: dict) -> str:
        payload = candidate.get("payload") or {}
        return payload.get("text", "")

    def _post_json(self, url: str, payload: dict, headers: dict[str, str]) -> dict | list:
        with httpx.Client(timeout=self.settings.http_timeout_seconds) as client:
            response = client.post(url, json=payload, headers=headers)
            response.raise_for_status()
            return response.json()

    @staticmethod
    def _build_headers(api_key: str | None) -> dict[str, str]:
        headers = {"Content-Type": "application/json"}
        if api_key:
            headers["Authorization"] = f"Bearer {api_key}"
        return headers

    @staticmethod
    def _join_url(base_url: str, path: str) -> str:
        normalized = base_url.rstrip("/") + "/"
        return urljoin(normalized, path.lstrip("/"))

    @staticmethod
    def _require_value(value: str | None, env_name: str) -> str:
        if value:
            return value
        raise ProviderConfigError(f"{env_name} must be set")

    @classmethod
    def _rerank_endpoint(cls, base_url: str) -> str:
        normalized = base_url.rstrip("/")
        if normalized.endswith("/rerank"):
            return normalized
        return cls._join_url(normalized, "/rerank")
