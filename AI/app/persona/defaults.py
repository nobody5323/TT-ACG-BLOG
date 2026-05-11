def build_persona_prefix(persona_id: str) -> str:
    if persona_id == "default-anime-assistant":
        return "You are the user's anime blog assistant."
    return f"Persona<{persona_id}> is active."
