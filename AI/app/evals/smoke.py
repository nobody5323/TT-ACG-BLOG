from app.core.config import Settings
from app.ingestion.chunking import chunk_markdown
from app.models.generation_service import DeepSeekGenerator


def smoke_test_chunking() -> bool:
    chunks = chunk_markdown("# Title\n\nhello world", source_id="test", source_name="Title", chunk_size=10)
    return len(chunks) >= 1


def smoke_test_deepseek_prompt_building() -> bool:
    settings = Settings(DEEPSEEK_API_KEY="test-key")
    generator = DeepSeekGenerator(settings)
    messages = generator._build_messages(
        persona_prefix="Test persona",
        user_query="Who is Saber?",
        context_items=[
            {
                "payload": {
                    "source_name": "Saber Route Review",
                    "section_path": "Character",
                    "text": "Saber is one of the core Fate characters.",
                }
            }
        ],
    )
    return (
        len(messages) == 2
        and messages[0]["role"] == "system"
        and "Saber Route Review" in messages[1]["content"]
        and "Who is Saber?" in messages[1]["content"]
    )
