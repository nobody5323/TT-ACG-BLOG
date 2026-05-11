from __future__ import annotations

from urllib.parse import urljoin

import httpx

from app.core.config import Settings
from app.models.vector_service import ProviderConfigError


class DeepSeekGenerator:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings

    def generate_answer(
        self,
        *,
        persona_prefix: str,
        user_query: str,
        context_items: list[dict],
    ) -> str:
        api_key = self.settings.deepseek_api_key
        if not api_key:
            raise ProviderConfigError("DEEPSEEK_API_KEY is required for DeepSeek generation")

        payload = {
            "model": self.settings.deepseek_model,
            "messages": self._build_messages(
                persona_prefix=persona_prefix,
                user_query=user_query,
                context_items=context_items,
            ),
        }
        data = self._post_json(self._join_url(self._base_url(), "/chat/completions"), payload, api_key)
        return self._extract_content(data)

    def _build_messages(
        self,
        *,
        persona_prefix: str,
        user_query: str,
        context_items: list[dict],
    ) -> list[dict[str, str]]:
        system_prompt = (
            f"{persona_prefix}\n\n"
            "You are an assistant for an ACG blog QA service. "
            "Prefer the retrieved blog context when it is available. "
            "If the context is insufficient, say so clearly instead of inventing blog facts. "
            "If you add general knowledge, distinguish it from blog-grounded claims. "
            "Answer naturally and directly."
        )

        user_sections: list[str] = []
        context_block = self._format_context(context_items)
        if context_block:
            user_sections.append("Retrieved blog context:\n" + context_block)
            user_sections.append(
                "Please answer using the retrieved context first. If it is insufficient, say that explicitly."
            )
        else:
            user_sections.append(
                "There is no retrieved blog context for this turn. "
                "Answer the user directly and avoid claiming unsupported blog-specific facts."
            )
        user_sections.append("User question:\n" + user_query)

        return [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": "\n\n".join(user_sections)},
        ]

    @staticmethod
    def _format_context(context_items: list[dict]) -> str:
        blocks: list[str] = []
        for index, item in enumerate(context_items, start=1):
            payload = item.get("payload") or {}
            text = (payload.get("text") or "").strip()
            if not text:
                continue
            title = payload.get("source_name") or f"Document {index}"
            section = payload.get("section_path") or ""
            header = f"[{index}] {title}"
            if section:
                header += f" | {section}"
            blocks.append(f"{header}\n{text}")
        return "\n\n".join(blocks)

    def _post_json(self, url: str, payload: dict, api_key: str) -> dict:
        headers = {
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
        }
        with httpx.Client(timeout=self.settings.http_timeout_seconds) as client:
            response = client.post(url, json=payload, headers=headers)
            response.raise_for_status()
            data = response.json()
        if not isinstance(data, dict):
            raise RuntimeError("Unexpected DeepSeek response shape")
        return data

    @staticmethod
    def _extract_content(data: dict) -> str:
        choices = data.get("choices") or []
        if not choices:
            raise RuntimeError("DeepSeek response did not contain any choices")

        message = choices[0].get("message") or {}
        content = message.get("content")
        if isinstance(content, str) and content.strip():
            return content.strip()

        if isinstance(content, list):
            text_parts = [
                item.get("text", "")
                for item in content
                if isinstance(item, dict) and item.get("type") == "text"
            ]
            merged = "".join(text_parts).strip()
            if merged:
                return merged

        raise RuntimeError("DeepSeek response did not contain text content")

    def _base_url(self) -> str:
        return self.settings.deepseek_base_url or "https://api.deepseek.com"

    @staticmethod
    def _join_url(base_url: str, path: str) -> str:
        normalized = base_url.rstrip("/") + "/"
        return urljoin(normalized, path.lstrip("/"))
