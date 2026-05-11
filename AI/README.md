# ACG Blog AI

Python backend for an ACG blog QA service built around:

- `deepseek-v4-flash`
- `Qwen/Qwen3-Embedding-4B`
- `Qwen/Qwen3-Reranker-4B`
- `Qdrant`
- `LangGraph`

## Current Capabilities

The current project includes:

- FastAPI app with `health`, `chat`, `sync`, and `ingest` routes
- API key protection for non-public routes
- Public blog knowledge retrieval from Qdrant
- User memory retrieval and write-back
- Query rewrite and lightweight intent routing
- Response safety review and source assembly
- Incremental blog DB sync with persisted cursor state
- File ingestion for `txt`, `md`, and `html`
- Local smoke and regression eval helpers

## Project Structure

- `app/api`: HTTP routes, dependency wiring, error handling
- `app/graph`: LangGraph workflow, state, and intent routing
- `app/retrieval`: public retrieval and rerank merge logic
- `app/memory`: user memory recall and write-back
- `app/ingestion`: blog sync and upload ingestion
- `app/storage`: Qdrant, blog DB, and sync state adapters
- `app/models`: generation, vector, and response services
- `app/evals`: smoke and regression eval runners
- `tests`: API and workflow regression tests

## Quick Start

1. Create a virtual environment
2. Install dependencies
3. Copy `.env.example` to `.env`
4. Set the required provider credentials
5. Start the API

```bash
python -m venv .venv
.venv\Scripts\activate
pip install -e .
uvicorn app.main:app --reload
```

## Minimal Environment

At minimum, set:

```env
APP_NAME=acg-blog-ai
APP_ENV=development
APP_HOST=0.0.0.0
APP_PORT=8000
LOG_LEVEL=INFO
API_AUTH_TOKEN=change-me

DEEPSEEK_API_KEY=sk-...
DEEPSEEK_MODEL=deepseek-v4-flash

QDRANT_URL=http://localhost:6333
QDRANT_COLLECTION_BLOG=blog_knowledge
QDRANT_COLLECTION_MEMORY=user_memory

EMBED_PROVIDER=stub
RERANK_PROVIDER=stub
VECTOR_SIZE=2560
```

Using `stub` for embedding and rerank is enough for local API bring-up and test flows. Real retrieval quality requires a real embedding and rerank provider.

## Provider Modes

Embedding providers:

- `stub`
- `qwen_cloud`
- `openai_compatible`
- `tei`

Rerank providers:

- `stub`
- `qwen_cloud`
- `openai_compatible`
- `tei`

### Qwen Cloud Embedding

```env
EMBED_PROVIDER=qwen_cloud
EMBED_MODEL=text-embedding-v4
EMBED_API_BASE=https://dashscope-intl.aliyuncs.com/compatible-mode/v1
EMBED_API_KEY=sk-...
EMBED_DIMENSIONS=1024
VECTOR_SIZE=1024
```

### Local TEI Embedding

```env
EMBED_PROVIDER=tei
EMBED_MODEL=Qwen/Qwen3-Embedding-4B
EMBED_API_BASE=http://localhost:8080
EMBED_DIMENSIONS=2560
VECTOR_SIZE=2560
```

### Local / Compatible Reranker

```env
RERANK_PROVIDER=openai_compatible
RERANK_MODEL=Qwen/Qwen3-Reranker-4B
RERANK_API_BASE=http://localhost:8001
```

The rerank service is expected to expose a `POST /rerank` style endpoint.

## Main Routes

- `GET /health`
- `POST /chat`
- `POST /sync/bootstrap-qdrant`
- `POST /sync/blog`
- `POST /ingest/upload`

`/chat`, `/sync/*`, and `/ingest/*` require `X-API-Key`.

## Running Tests

```bash
pytest tests/test_api.py
```

## Evals

Available helpers:

- `app.evals.smoke_test_chunking`
- `app.evals.smoke_test_deepseek_prompt_building`
- `app.evals.run_regression_eval`

The regression eval is deterministic and local. It is designed for workflow regression checks, not real provider benchmarking.

## Known Gaps

- Upload ingestion does not yet support `PDF` or `DOCX`
- Sync cursor state is stored in a local JSON file, not a DB table
- Full end-to-end runtime validation against live Qdrant and live providers is not included in tests
