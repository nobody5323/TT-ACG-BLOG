from __future__ import annotations


class ResponseSafetyService:
    BLOCK_PATTERNS = (
        "api key",
        "password",
        "token",
        "bypass auth",
        "exploit",
        "sql injection",
    )

    def review_and_finalize(
        self,
        *,
        user_query: str,
        answer: str,
        context_items: list[dict],
        citations: list[str],
    ) -> dict[str, object]:
        lowered_query = user_query.lower()
        if any(pattern in lowered_query for pattern in self.BLOCK_PATTERNS):
            return {
                "final_response": (
                    "I can't help with credential theft, bypassing authentication, or exploit instructions."
                ),
                "safety_blocked": True,
            }

        evidence_strength = self._estimate_evidence_strength(context_items)
        cleaned_answer = answer.strip()

        if not context_items and self._looks_like_blog_specific_question(user_query):
            return {
                "final_response": (
                    "I don't have enough retrieved blog evidence for that yet. "
                    "Try giving the article title, character name, or a more specific keyword."
                ),
                "safety_blocked": False,
            }

        if context_items and evidence_strength < 0.15:
            prefix = "The retrieved evidence is weak, so treat this as tentative.\n\n"
            cleaned_answer = prefix + cleaned_answer

        finalized = self._append_citations(cleaned_answer, citations)
        return {
            "final_response": finalized,
            "safety_blocked": False,
        }

    @staticmethod
    def _estimate_evidence_strength(context_items: list[dict]) -> float:
        if not context_items:
            return 0.0
        scores: list[float] = []
        for item in context_items:
            score = item.get("rerank_score", item.get("score", 0.0))
            if isinstance(score, (int, float)):
                scores.append(float(score))
        if not scores:
            return 0.0
        return max(scores)

    @staticmethod
    def _looks_like_blog_specific_question(user_query: str) -> bool:
        lowered = user_query.lower()
        triggers = ("blog", "article", "post", "review", "what did you write", "which post")
        return any(trigger in lowered for trigger in triggers)

    @staticmethod
    def _append_citations(answer: str, citations: list[str]) -> str:
        seen: list[str] = []
        for item in citations:
            if item and item not in seen:
                seen.append(item)
        if not seen:
            return answer
        return f"{answer}\n\nSources: " + ", ".join(seen[:4])
