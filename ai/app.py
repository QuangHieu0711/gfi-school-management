from fastapi import FastAPI
from pydantic import BaseModel
from typing import Optional

from model_service import generate_comment

app = FastAPI(title="GFI Comment AI Service")


class CommentRequest(BaseModel):
    """Request body sent by the backend to generate one student comment."""

    grade_level: str
    subject_name: str
    term: str
    week_no: Optional[int] = None
    lesson_no: Optional[int] = None
    lesson_title: Optional[str] = ""
    learning_objective: Optional[str] = ""
    evaluation: str
    attendance_full: Optional[int] = 1
    participation_level: Optional[str] = ""
    behavior_tag: Optional[str] = ""
    textbook_series: Optional[str] = ""


# Do not preload the model at startup. Loading the model at process startup
# caused segmentation faults on some environments (low-memory / Windows).
# The model will be loaded lazily on first request by `generate_comment`.


@app.get("/health")
def health():
    """Simple health check for backend/service monitoring."""
    return {"status": "ok"}


@app.post("/generate-comment")
def generate_comment_api(request: CommentRequest):
    """Generate comment_text for one lesson/student context."""
    # Support both Pydantic v1 and v2.
    data = request.model_dump() if hasattr(request, "model_dump") else request.dict()
    comment_text = generate_comment(data)
    return {"comment_text": comment_text}
