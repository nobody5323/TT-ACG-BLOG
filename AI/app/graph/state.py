from typing import TypedDict


class ChatState(TypedDict, total=False):
    user_id: str
    session_id: str
    persona_id: str
    user_query: str
    rewritten_query: str
    intent: str
    should_retrieve: bool
    public_context: list[dict]
    memory_context: list[dict]
    combined_context: list[dict]
    citations: list[str]
    used_rerank: bool
    memory_written: bool
    raw_response: str
    safety_blocked: bool
    final_response: str
