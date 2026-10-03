# Recall API

FastAPI service for parsing a single PDF/TXT study material and grading checkpoint recall with Groq. It exposes only `POST /materials` and `POST /checkpoint/score`; API docs are disabled.

## Local development

```bash
cd backend
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env
```

Keep `DEV_AUTH=1` for local-only development. Set `GROQ_API_KEY` and optionally `GROQ_MODEL` in `.env`, then run:

```bash
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
pytest -q
```

With `DEV_AUTH=1`, requests can omit auth and use the default `dev-user`, or pass `X-Dev-User` to isolate users. Never use development auth on a public deployment. The app rate limits each user per process. Material text is held only in memory for three hours, so a restart expires material IDs; Android re-uploads its private local copy and retries when it sees `material_not_found`.

For manual API checks, upload multipart fields `file` and `topic` to `/materials`, then POST JSON `{ "materialId": "...", "fromPage": 1, "toPage": 1, "summary": "..." }` to `/checkpoint/score`. In real mode, send `Authorization: Bearer <Firebase ID token>` on both requests.

## Railway deployment

1. Create a Railway service from this repository and set its **Root Directory** to `/backend`.
2. Set the start command to `uvicorn main:app --host 0.0.0.0 --port $PORT`.
3. Add Railway variables `DEV_AUTH=0`, `GROQ_API_KEY`, `GROQ_MODEL`, and `FIREBASE_PROJECT_ID`.
4. Create a Firebase service account with token-verification permissions and add its full JSON document as the secret variable `FIREBASE_SERVICE_ACCOUNT_JSON`. Do not put that JSON in the repo or `.env.example`.
5. Deploy. Set Android's `FOCUSMAXXING_BACKEND_URL` to the deployed base URL (without a trailing slash).

Railway's ephemeral process memory stores extracted materials and per-process rate-limit buckets. Reuploads after restarts are expected; multiple replicas each have their own rate-limit bucket.
