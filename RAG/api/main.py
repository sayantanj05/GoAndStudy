"""FastAPI service for Self-Learning RAG Chatbot."""
import os
import sys
import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

# Add project root to path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from RAG.api.routers import chat

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Startup and shutdown events."""
    logger.info("RAG Service starting up...")
    yield
    logger.info("RAG Service shutting down...")


app = FastAPI(
    title="GoAndStudy RAG Chatbot Service",
    description="Self-Learning RAG with Knowledge Reflection",
    version="1.0.0",
    lifespan=lifespan
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(chat.router, prefix="/api/v1/chat", tags=["chat"])


@app.get("/health")
async def health_check():
    return {"status": "healthy", "service": "rag-chatbot"}


@app.get("/")
async def root():
    return {
        "service": "GoAndStudy RAG Chatbot",
        "version": "1.0.0",
        "features": [
            "self-learning",
            "knowledge-reflection",
            "hybrid-retrieval",
            "feedback-loop"
        ]
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8002)
