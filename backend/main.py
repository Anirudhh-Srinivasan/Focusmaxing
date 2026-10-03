from __future__ import annotations

import json
import os
import re
import threading
import time
import uuid
from collections import defaultdict, deque
from dataclasses import dataclass
from io import BytesIO
from typing import Callable

from fastapi import Depends, FastAPI, File, Form, Header, HTTPException, UploadFile
from dotenv import load_dotenv
from groq import Groq
from pydantic import BaseModel, ConfigDict, Field, field_validator
from pypdf import PdfReader

load_dotenv()

from constants import (
    DEFAULT_GROQ_MODEL,
    MAX_CONTEXT_WORDS,
    MAX_FEEDBACK_SENTENCES,
    MAX_MISSED_POINTS,
    MAX_PDF_PAGES,
    MAX_RANGE_PAGES,
    MAX_UPLOAD_BYTES,
    MATERIAL_TTL_SECONDS,
    RATE_LIMIT_REQUESTS,
    RATE_LIMIT_WINDOW_SECONDS,
    SAMPLED_EXCERPT_COUNT,
    SUMMARY_MAX_WORDS,
    SUMMARY_MIN_WORDS,
    TXT_WORDS_PER_PAGE,
)

app = FastAPI(title="Focusmaxxing Recall API", docs_url=None, redoc_url=None, openapi_url=None)
WORD_RE = re.compile(r"\b[\w'-]+\b", re.UNICODE)


@dataclass(frozen=True)
class Material:
    owner_uid: str
    topic: str
    pages: list[str]
    page_word_counts: list[int]
    created_at: float


materials: dict[str, Material] = {}
_material_lock = threading.Lock()
_rate_lock = threading.Lock()
_rate_events: dict[str, deque[float]] = defaultdict(deque)
_groq_client: Groq | None = None
_firebase_app = None
_firebase_lock = threading.Lock()


class MaterialResponse(BaseModel):
    materialId: str
    pageCount: int = Field(ge=1)
    pageWordCounts: list[int]


class ScoreRequest(BaseModel):
    materialId: str = Field(min_length=1, max_length=100)
    fromPage: int = Field(ge=1)
    toPage: int = Field(ge=1)
    summary: str = Field(min_length=1, max_length=5_000)


class GradeResponse(BaseModel):
    model_config = ConfigDict(extra="ignore")
    recallScore: int = Field(ge=0, le=100)
    feedback: str = Field(min_length=1, max_length=600)
    keyPointsMissed: list[str] = Field(default_factory=list, max_length=MAX_MISSED_POINTS)

    @field_validator("feedback")
    @classmethod
    def feedback_at_most_two_sentences(cls, value: str) -> str:
        sentences = [part for part in re.split(r"(?<=[.!?])\s+", value.strip()) if part]
        if len(sentences) > MAX_FEEDBACK_SENTENCES:
            raise ValueError("feedback must contain at most two sentences")
        return value.strip()

    @field_validator("keyPointsMissed")
    @classmethod
    def trim_missed_points(cls, value: list[str]) -> list[str]:
        return [point.strip()[:300] for point in value if point.strip()]


def count_words(text: str) -> int:
    return len(WORD_RE.findall(text))


def split_txt_pages(text: str, words_per_page: int = TXT_WORDS_PER_PAGE) -> list[str]:
    words = text.split()
    if not words:
        raise ValueError("The text file is empty.")
    return [" ".join(words[index:index + words_per_page]) for index in range(0, len(words), words_per_page)]


def extract_material(filename: str, content_type: str | None, data: bytes) -> list[str]:
    suffix = (filename.rsplit(".", 1)[-1] if "." in filename else "").lower()
    if suffix == "txt" and content_type in (None, "", "text/plain", "application/octet-stream"):
        try:
            text = data.decode("utf-8-sig")
        except UnicodeDecodeError as exc:
            raise ValueError("Text files must use UTF-8 encoding.") from exc
        return split_txt_pages(text)
    if suffix == "pdf" and content_type in (None, "", "application/pdf", "application/octet-stream"):
        try:
            reader = PdfReader(BytesIO(data), strict=False)
            if reader.is_encrypted:
                raise ValueError("Password-protected PDFs are not supported.")
            if len(reader.pages) > MAX_PDF_PAGES:
                raise ValueError(f"PDFs may contain at most {MAX_PDF_PAGES} pages.")
            pages = [(page.extract_text() or "").strip() for page in reader.pages]
        except ValueError:
            raise
        except Exception as exc:
            raise ValueError("This PDF could not be read. Try a text-based PDF.") from exc
        if not pages or sum(count_words(page) for page in pages) == 0:
            raise ValueError("This PDF has no extractable text. Scanned PDFs and image-only pages are not supported.")
        return pages
    raise ValueError("Choose one PDF or plain-text (.txt) file.")


def sample_range_text(pages: list[str], max_words: int = MAX_CONTEXT_WORDS) -> str:
    """Use only the selected pages; for long ranges include evenly spaced excerpts."""
    source = "\n\n".join(pages)
    tokens = list(WORD_RE.finditer(source))
    if len(tokens) <= max_words:
        return source
    excerpt_count = min(SAMPLED_EXCERPT_COUNT, max_words)
    excerpt_words = max(1, max_words // excerpt_count)
    last_start = max(0, len(tokens) - excerpt_words)
    if excerpt_count == 1:
        starts = [last_start // 2]
    else:
        starts = [round(index * last_start / (excerpt_count - 1)) for index in range(excerpt_count)]
    chunks = [source[tokens[start].start():tokens[start + excerpt_words - 1].end()] for start in starts]
    return "\n\n[… ]\n\n".join(chunks)


def purge_expired_materials(now: float | None = None) -> None:
    current = time.time() if now is None else now
    with _material_lock:
        expired = [key for key, value in materials.items() if current - value.created_at >= MATERIAL_TTL_SECONDS]
        for key in expired:
            materials.pop(key, None)


def _verify_firebase_token(authorization: str | None) -> str:
    token = authorization.removeprefix("Bearer ").strip() if authorization else ""
    if not token:
        raise HTTPException(status_code=401, detail="A Firebase ID token is required.")
    try:
        import firebase_admin
        from firebase_admin import auth, credentials

        global _firebase_app
        with _firebase_lock:
            if _firebase_app is None:
                service_account_json = os.getenv("FIREBASE_SERVICE_ACCOUNT_JSON", "").strip()
                project_id = os.getenv("FIREBASE_PROJECT_ID")
                if service_account_json:
                    service_account = json.loads(service_account_json)
                    credential = credentials.Certificate(service_account)
                    options = {"projectId": project_id} if project_id else None
                    _firebase_app = firebase_admin.initialize_app(credential, options)
                else:
                    options = {"projectId": project_id} if project_id else None
                    _firebase_app = firebase_admin.initialize_app(options=options)
        decoded = auth.verify_id_token(token, app=_firebase_app)
        return str(decoded["uid"])
    except HTTPException:
        raise
    except Exception as exc:
        raise HTTPException(status_code=401, detail="The Firebase ID token is invalid or Firebase Admin is not configured.") from exc


def authenticated_user(
    authorization: str | None = Header(default=None),
    x_dev_user: str | None = Header(default=None),
) -> str:
    if os.getenv("DEV_AUTH", "0") == "1":
        uid = (x_dev_user or "dev-user").strip()
        if not uid or len(uid) > 128:
            raise HTTPException(status_code=400, detail="Invalid development user id.")
    else:
        uid = _verify_firebase_token(authorization)
    check_rate_limit(uid)
    return uid


def check_rate_limit(uid: str, now: float | None = None) -> None:
    current = time.time() if now is None else now
    with _rate_lock:
        events = _rate_events[uid]
        while events and current - events[0] >= RATE_LIMIT_WINDOW_SECONDS:
            events.popleft()
        if len(events) >= RATE_LIMIT_REQUESTS:
            raise HTTPException(status_code=429, detail="Rate limit reached. Please wait before trying again.")
        events.append(current)


def groq_client() -> Groq:
    global _groq_client
    if _groq_client is None:
        api_key = os.getenv("GROQ_API_KEY", "").strip()
        if not api_key:
            raise HTTPException(status_code=503, detail="The recall grader is not configured.")
        _groq_client = Groq(api_key=api_key)
    return _groq_client


def _llm_json(context: str, summary: str) -> str:
    response = groq_client().chat.completions.create(
        model=os.getenv("GROQ_MODEL", DEFAULT_GROQ_MODEL),
        temperature=0,
        response_format={"type": "json_object"},
        messages=[
            {
                "role": "system",
                "content": (
                    "Grade a learner's recall fairly using only the supplied selected-range context. "
                    "Judge factual accuracy and whether the summary captures important ideas across the whole claimed range; "
                    "a very broad range summarized from only one small part should score lower. Content outside this range does not count. "
                    "Never accuse or speculate about dishonesty. Return JSON with integer recallScore from 0 to 100, "
                    "feedback of at most two short sentences, and keyPointsMissed as an array of at most five concise strings."
                ),
            },
            {"role": "user", "content": f"SELECTED MATERIAL RANGE (only grading context):\n{context}\n\nLEARNER RECALL:\n{summary}"},
        ],
    )
    return response.choices[0].message.content or ""


def grade_with_retry(context: str, summary: str, generate: Callable[[str, str], str] = _llm_json) -> GradeResponse:
    last_error: Exception | None = None
    for _ in range(2):
        try:
            return GradeResponse.model_validate_json(generate(context, summary))
        except Exception as exc:
            last_error = exc
    raise ValueError("The recall grader returned an invalid response.") from last_error


@app.post("/materials", response_model=MaterialResponse)
async def upload_material(
    file: UploadFile = File(...),
    topic: str = Form(..., min_length=1, max_length=120),
    uid: str = Depends(authenticated_user),
) -> MaterialResponse:
    purge_expired_materials()
    clean_topic = topic.strip()
    if not clean_topic:
        raise HTTPException(status_code=422, detail="Enter a topic name.")
    data = await file.read(MAX_UPLOAD_BYTES + 1)
    if len(data) > MAX_UPLOAD_BYTES:
        raise HTTPException(status_code=413, detail=f"Files must be no larger than {MAX_UPLOAD_BYTES // (1024 * 1024)} MB.")
    try:
        pages = extract_material(file.filename or "", file.content_type, data)
    except ValueError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    if (file.filename or "").lower().endswith(".pdf") and len(pages) > MAX_PDF_PAGES:
        raise HTTPException(status_code=422, detail=f"Materials may contain at most {MAX_PDF_PAGES} pages.")
    counts = [count_words(page) for page in pages]
    material_id = str(uuid.uuid4())
    with _material_lock:
        materials[material_id] = Material(uid, clean_topic, pages, counts, time.time())
    return MaterialResponse(materialId=material_id, pageCount=len(pages), pageWordCounts=counts)


@app.post("/checkpoint/score")
def score_checkpoint(payload: ScoreRequest, uid: str = Depends(authenticated_user)) -> dict:
    purge_expired_materials()
    with _material_lock:
        material = materials.get(payload.materialId)
    if material is None or material.owner_uid != uid:
        raise HTTPException(status_code=404, detail={"code": "material_not_found", "message": "Material is no longer available. Upload it again."})
    page_count = len(material.pages)
    if payload.toPage > page_count:
        raise HTTPException(status_code=422, detail=f"Page numbers must be between 1 and {page_count}.")
    if payload.toPage < payload.fromPage:
        raise HTTPException(status_code=422, detail="The ending page must be at or after the starting page.")
    if payload.toPage - payload.fromPage + 1 > MAX_RANGE_PAGES:
        raise HTTPException(status_code=422, detail=f"A checkpoint can cover at most {MAX_RANGE_PAGES} pages.")
    summary_words = count_words(payload.summary)
    if not SUMMARY_MIN_WORDS <= summary_words <= SUMMARY_MAX_WORDS:
        raise HTTPException(status_code=422, detail=f"Recall must be between {SUMMARY_MIN_WORDS} and {SUMMARY_MAX_WORDS} words.")

    selected_pages = material.pages[payload.fromPage - 1:payload.toPage]
    context = sample_range_text(selected_pages)
    words_covered = sum(material.page_word_counts[payload.fromPage - 1:payload.toPage])
    try:
        grade = grade_with_retry(context, payload.summary)
    except HTTPException:
        raise
    except Exception as exc:
        raise HTTPException(status_code=502, detail="Recall grading failed. Please try this checkpoint again.") from exc
    return {
        "recallScore": grade.recallScore,
        "wordsCovered": words_covered,
        "feedback": grade.feedback,
        "keyPointsMissed": grade.keyPointsMissed,
    }
