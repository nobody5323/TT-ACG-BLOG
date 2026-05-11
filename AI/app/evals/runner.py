from __future__ import annotations

from dataclasses import dataclass

from app.core.config import Settings
from app.graph.workflow import build_workflow


@dataclass
class EvalCase:
    name: str
    input_state: dict
    expected_substrings: list[str]
    expected_flags: dict[str, object]


@dataclass
class EvalResult:
    name: str
    passed: bool
    details: str


def run_eval_cases(
    *,
    settings: Settings,
    retrieval_service,
    memory_service,
    generator_patch,
    cases: list[EvalCase],
) -> list[EvalResult]:
    workflow = build_workflow(
        settings=settings,
        retrieval_service=retrieval_service,
        memory_service=memory_service,
    )
    generator_patch()

    results: list[EvalResult] = []
    for case in cases:
        state = workflow.invoke(case.input_state)
        failures: list[str] = []

        final_response = state.get("final_response", "")
        for substring in case.expected_substrings:
            if substring not in final_response:
                failures.append(f"missing substring: {substring}")

        for key, expected_value in case.expected_flags.items():
            if state.get(key) != expected_value:
                failures.append(f"{key}={state.get(key)!r}, expected {expected_value!r}")

        results.append(
            EvalResult(
                name=case.name,
                passed=not failures,
                details="; ".join(failures) if failures else "ok",
            )
        )
    return results
