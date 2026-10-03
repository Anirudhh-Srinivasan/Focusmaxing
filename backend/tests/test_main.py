import json

import pytest
from fastapi.testclient import TestClient
from fastapi import HTTPException

import main
from constants import MAX_CONTEXT_WORDS, RATE_LIMIT_REQUESTS, RATE_LIMIT_WINDOW_SECONDS, TXT_WORDS_PER_PAGE


@pytest.fixture(autouse=True)
def dev_mode(monkeypatch):
    monkeypatch.setenv("DEV_AUTH", "1")
    main.materials.clear()
    main._rate_events.clear()


@pytest.fixture
def client():
    return TestClient(main.app)


def test_txt_splits_into_virtual_pages_and_counts_words():
    pages = main.split_txt_pages(" ".join(f"word{i}" for i in range(TXT_WORDS_PER_PAGE + 7)))
    assert len(pages) == 2
    assert main.count_words(pages[0]) == TXT_WORDS_PER_PAGE
    assert main.count_words(pages[1]) == 7


def test_range_sampling_preserves_beginning_middle_and_end():
    pages = [" ".join(f"word{i}" for i in range(MAX_CONTEXT_WORDS * 3))]
    sampled = main.sample_range_text(pages)
    assert "word0" in sampled
    assert any(f"word{index}" in sampled for index in range(MAX_CONTEXT_WORDS * 3 // 2 - 1_000, MAX_CONTEXT_WORDS * 3 // 2 + 1_000))
    assert f"word{MAX_CONTEXT_WORDS * 3 - 1}" in sampled
    assert main.count_words(sampled) <= MAX_CONTEXT_WORDS


def test_short_range_is_returned_without_sampling():
    pages = ["alpha beta", "middle words", "last page"]
    assert main.sample_range_text(pages) == "alpha beta\n\nmiddle words\n\nlast page"


def test_grade_json_validation_and_one_retry():
    outputs = iter([
        '{"recallScore":"excellent","feedback":"Not a number","keyPointsMissed":[]}',
        json.dumps({"recallScore": 76, "feedback": "Good coverage. Add one more detail.", "keyPointsMissed": ["One detail"]}),
    ])
    calls = []
    grade = main.grade_with_retry("context", "summary", lambda context, summary: calls.append((context, summary)) or next(outputs))
    assert grade.recallScore == 76
    assert len(calls) == 2
    with pytest.raises(ValueError):
        main.GradeResponse.model_validate_json(json.dumps({"recallScore": 101, "feedback": "Too high", "keyPointsMissed": []}))
    with pytest.raises(ValueError):
        main.GradeResponse.model_validate_json(json.dumps({"recallScore": 30, "feedback": "One. Two. Three.", "keyPointsMissed": []}))


def test_material_upload_and_missing_material_response(client, monkeypatch):
    response = client.post("/materials", data={"topic": "Algebra"}, files={"file": ("notes.txt", b"one two three", "text/plain")})
    assert response.status_code == 200
    data = response.json()
    assert data["pageCount"] == 1
    assert data["pageWordCounts"] == [3]
    missing = client.post("/checkpoint/score", json={"materialId": "expired", "fromPage": 1, "toPage": 1, "summary": "a recall summary long enough to get past this layer"})
    assert missing.status_code == 404
    assert missing.json()["detail"]["code"] == "material_not_found"


def test_rejects_scanned_pdf_clearly(client, monkeypatch):
    from pypdf import PdfWriter
    from io import BytesIO

    output = BytesIO()
    writer = PdfWriter()
    writer.add_blank_page(width=100, height=100)
    writer.write(output)
    response = client.post("/materials", data={"topic": "Scanned"}, files={"file": ("scan.pdf", output.getvalue(), "application/pdf")})
    assert response.status_code == 422
    assert "no extractable text" in response.json()["detail"]


def test_owner_is_required_for_material_score(client, monkeypatch):
    uploaded = client.post("/materials", headers={"X-Dev-User": "alice"}, data={"topic": "Bio"}, files={"file": ("bio.txt", b"text", "text/plain")})
    material_id = uploaded.json()["materialId"]
    response = client.post("/checkpoint/score", headers={"X-Dev-User": "bob"}, json={"materialId": material_id, "fromPage": 1, "toPage": 1, "summary": "This summary is deliberately over fifteen words for validation in this endpoint."})
    assert response.status_code == 404


def test_rate_limit_is_per_user_and_expires(monkeypatch):
    for _ in range(RATE_LIMIT_REQUESTS):
        main.check_rate_limit("alice", now=100)
    with pytest.raises(HTTPException) as error:
        main.check_rate_limit("alice", now=100)
    assert error.value.status_code == 429
    main.check_rate_limit("bob", now=100)
    main.check_rate_limit("alice", now=100 + RATE_LIMIT_WINDOW_SECONDS)
