from __future__ import annotations

import re


class IntentService:
    SMALLTALK_PATTERNS = (
        r"^hi[.! ]*$",
        r"^hello[.! ]*$",
        r"^hey[.! ]*$",
        r"^thanks[.! ]*$",
        r"^thank you[.! ]*$",
        r"^good morning[.! ]*$",
        r"^good night[.! ]*$",
    )

    def classify(self, user_query: str) -> dict[str, object]:
        normalized = self.rewrite_query(user_query)
        lowered = normalized.lower()
        is_smalltalk = any(re.match(pattern, lowered) for pattern in self.SMALLTALK_PATTERNS)
        return {
            "intent": "smalltalk" if is_smalltalk else "knowledge_qa",
            "rewritten_query": normalized,
            "should_retrieve": not is_smalltalk,
        }

    @staticmethod
    def rewrite_query(user_query: str) -> str:
        normalized = " ".join(user_query.strip().split())
        normalized = normalized.replace("？", "?").replace("。", ".").replace("，", ",")
        return normalized
