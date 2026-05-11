from __future__ import annotations

import logging

from langgraph.graph import END, START, StateGraph

from app.core.config import Settings
from app.graph.intent import IntentService
from app.graph.state import ChatState
from app.memory.service import MemoryService
from app.models.generation_service import DeepSeekGenerator
from app.models.response_service import ResponseSafetyService
from app.persona.defaults import build_persona_prefix
from app.retrieval.service import RetrievalService


logger = logging.getLogger(__name__)


def build_workflow(
    settings: Settings,
    retrieval_service: RetrievalService,
    memory_service: MemoryService,
):
    graph = StateGraph(ChatState)
    generator = DeepSeekGenerator(settings)
    intent_service = IntentService()
    response_safety_service = ResponseSafetyService()

    def prepare_query_node(state: ChatState) -> ChatState:
        decision = intent_service.classify(state["user_query"])
        return {
            "intent": str(decision["intent"]),
            "rewritten_query": str(decision["rewritten_query"]),
            "should_retrieve": bool(decision["should_retrieve"]),
        }

    def public_retrieve_node(state: ChatState) -> ChatState:
        if not state.get("should_retrieve", True):
            return {"public_context": []}
        try:
            public_context = retrieval_service.retrieve_public_context(state["rewritten_query"])
        except Exception as exc:
            logger.warning("Retrieval failed, falling back to generation without context: %s", exc)
            return {
                "public_context": [],
            }
        return {"public_context": public_context}

    def memory_retrieve_node(state: ChatState) -> ChatState:
        if not state.get("should_retrieve", True):
            return {"memory_context": []}
        try:
            memory_context = memory_service.retrieve_memories(
                query=state["rewritten_query"],
                user_id=state["user_id"],
                persona_id=state.get("persona_id") or settings.default_persona_id,
            )
        except Exception as exc:
            logger.warning("Memory retrieval failed, continuing without memory context: %s", exc)
            return {"memory_context": []}
        return {"memory_context": memory_context}

    def assemble_context_node(state: ChatState) -> ChatState:
        combined_context = retrieval_service.merge_contexts(
            query=state.get("rewritten_query", state["user_query"]),
            public_context=state.get("public_context", []),
            memory_context=state.get("memory_context", []),
        )
        citations: list[str] = []
        for item in combined_context:
            payload = item.get("payload") or {}
            source_name = payload.get("source_name") or payload.get("memory_type") or ""
            if source_name:
                citations.append(source_name)
        return {
            "combined_context": combined_context,
            "citations": citations,
            "used_rerank": bool(combined_context),
        }

    def generate_node(state: ChatState) -> ChatState:
        persona_prefix = build_persona_prefix(state.get("persona_id") or settings.default_persona_id)
        answer = generator.generate_answer(
            persona_prefix=persona_prefix,
            user_query=state["user_query"],
            context_items=state.get("combined_context", []),
        )
        return {"raw_response": answer}

    def review_response_node(state: ChatState) -> ChatState:
        reviewed = response_safety_service.review_and_finalize(
            user_query=state["user_query"],
            answer=state.get("raw_response", ""),
            context_items=state.get("combined_context", []),
            citations=state.get("citations", []),
        )
        return {
            "final_response": str(reviewed["final_response"]),
            "safety_blocked": bool(reviewed["safety_blocked"]),
        }

    def write_memory_node(state: ChatState) -> ChatState:
        if state.get("safety_blocked"):
            return {"memory_written": False}
        written = memory_service.write_memory(
            user_id=state["user_id"],
            persona_id=state.get("persona_id") or settings.default_persona_id,
            user_message=state["user_query"],
        )
        return {"memory_written": written is not None}

    graph.add_node("prepare_query", prepare_query_node)
    graph.add_node("public_retrieve", public_retrieve_node)
    graph.add_node("memory_retrieve", memory_retrieve_node)
    graph.add_node("assemble_context", assemble_context_node)
    graph.add_node("generate", generate_node)
    graph.add_node("review_response", review_response_node)
    graph.add_node("write_memory", write_memory_node)
    graph.add_edge(START, "prepare_query")
    graph.add_edge("prepare_query", "public_retrieve")
    graph.add_edge("public_retrieve", "memory_retrieve")
    graph.add_edge("memory_retrieve", "assemble_context")
    graph.add_edge("assemble_context", "generate")
    graph.add_edge("generate", "review_response")
    graph.add_edge("review_response", "write_memory")
    graph.add_edge("write_memory", END)
    return graph.compile()
