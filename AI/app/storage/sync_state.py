from __future__ import annotations

import json
from pathlib import Path

from app.core.config import Settings
from app.models.schemas import BlogSyncState


class SyncStateStore:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        self.path = Path(settings.blog_sync_state_path)

    def load(self) -> BlogSyncState:
        if not self.path.exists():
            return BlogSyncState()
        data = json.loads(self.path.read_text(encoding="utf-8"))
        return BlogSyncState(**data)

    def save(self, state: BlogSyncState) -> None:
        self.path.parent.mkdir(parents=True, exist_ok=True)
        self.path.write_text(
            json.dumps(state.model_dump(), ensure_ascii=False, indent=2),
            encoding="utf-8",
        )
