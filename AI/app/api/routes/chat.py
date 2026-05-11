from typing import Annotated, Any

from fastapi import APIRouter, Depends

from app.api.deps import get_app_settings, get_workflow
from app.api.errors import DEFAULT_ERROR_RESPONSES, translate_service_exception
from app.chat_history import try_append_chat_history
from app.core.config import Settings
from app.models.schemas import ChatRequest, ChatResponse


router = APIRouter()


@router.post("", response_model=ChatResponse, responses=DEFAULT_ERROR_RESPONSES)
def chat(
    request: ChatRequest,
    settings: Annotated[Settings, Depends(get_app_settings)],
    workflow: Annotated[Any, Depends(get_workflow)],
) -> ChatResponse:
    try:
        state = workflow.invoke(
            {
                "user_id": request.user_id,
                "session_id": request.session_id,
                "persona_id": request.persona_id,
                "user_query": request.message,
            }
        )
    except Exception as exc:
        try_append_chat_history(
            settings,
            {
                "user_id": request.user_id,
                "session_id": request.session_id,
                "persona_id": request.persona_id,
                "message": request.message,
                "status": "error",
                "error": str(exc),
            },
        )
        raise translate_service_exception(
            exc,
            default_code="chat_failed",
            default_message="Chat request failed",
        ) from exc

    response = ChatResponse(
        answer=state["final_response"],
        citations=state.get("citations", []),
        used_rerank=state.get("used_rerank", False),
    )
    try_append_chat_history(
        settings,
        {
            "user_id": request.user_id,
            "session_id": request.session_id,
            "persona_id": request.persona_id,
            "message": request.message,
            "status": "ok",
            "answer": response.answer,
            "citations": response.citations,
            "used_rerank": response.used_rerank,
        },
    )
    return response
