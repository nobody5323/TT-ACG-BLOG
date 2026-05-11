from __future__ import annotations

import json
import logging
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from app.core.config import Settings


logger = logging.getLogger(__name__)


def append_chat_history(settings: Settings, record: dict[str, Any]) -> None:
    path = Path(settings.chat_log_path)
    path.parent.mkdir(parents=True, exist_ok=True)
    payload = {
        "timestamp": datetime.now(timezone.utc).isoformat(),
        **record,
    }
    with path.open("a", encoding="utf-8") as handle:
        handle.write(json.dumps(payload, ensure_ascii=False) + "\n")


def try_append_chat_history(settings: Settings, record: dict[str, Any]) -> None:
    try:
        append_chat_history(settings, record)
    except Exception:
        logger.warning("Failed to append chat history", exc_info=True)
