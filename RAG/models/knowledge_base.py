"""
Pydantic models for the Self-Learning RAG System.
Defines data structures for documents, chunks, reflections, and evaluations.
"""

from datetime import datetime
from typing import List, Optional, Dict, Any, Literal
from pydantic import BaseModel, Field
from enum import Enum


class DocumentStatus(str, Enum):
    PENDING = "pending"
    PROCESSING = "processing"
    PROCESSED = "processed"
    FAILED = "failed"
    ARCHIVED = "archived"


class DocumentType(str, Enum):
    PDF = "pdf"
    DOCX = "docx"
    TXT = "txt"
    MD = "md"
    HTML = "html"
    BOOK_METADATA = "book_metadata"
    LIBRARY_POLICY = "library_policy"
    FAQ = "faq"
    EXTERNAL = "external"


class ReflectionType(str, Enum):
    SEMANTIC_SIMILARITY = "semantic_similarity"
    ENTITY_OVERLAP = "entity_overlap"
    TEMPORAL_SEQUENCE = "temporal_sequence"
    CONTRADICTION = "contradiction"
    SUPPORTING = "supporting"
    EXTENDING = "extending"


class FeedbackType(str, Enum):
    THUMBS_UP = "thumbs_up"
    THUMBS_DOWN = "thumbs_down"
    HELPFUL = "helpful"
    NOT_HELPFUL = "not_helpful"
    OUTDATED = "outdated"
    INCOMPLETE = "incomplete"


# ==================== KNOWLEDGE DOCUMENT ====================

class KnowledgeDocument(BaseModel):
    """A document in the knowledge base."""
    id: Optional[str] = None
    title: str = Field(..., description="Document title")
    content: str = Field(..., description="Full document content")
    doc_type: DocumentType = DocumentType.TXT
    status: DocumentStatus = DocumentStatus.PENDING
    
    # Metadata
    source_url: Optional[str] = None
    file_path: Optional[str] = None
    author: Optional[str] = None
    category: Optional[str] = None
    tags: List[str] = Field(default_factory=list)
    
    # NLP extracted metadata
    summary: Optional[str] = None
    entities: List[Dict[str, Any]] = Field(default_factory=list)
    keywords: List[str] = Field(default_factory=list)
    topics: List[str] = Field(default_factory=list)
    
    # Statistics
    word_count: int = 0
    chunk_count: int = 0
    
    # Timestamps
    created_at: datetime = Field(default_factory=datetime.utcnow)
    updated_at: datetime = Field(default_factory=datetime.utcnow)
    processed_at: Optional[datetime] = None
    
    # Versioning
    version: int = 1
    parent_doc_id: Optional[str] = None
    is_latest: bool = True
    
    # Usage metrics
    retrieval_count: int = 0
    feedback_score: Optional[float] = None
    
    class Config:
        json_schema_extra = {
            "example": {
                "title": "Library Borrowing Policy 2024",
                "content": "Members can borrow up to 5 books...",
                "doc_type": "library_policy",
                "category": "policies",
                "tags": ["borrowing", "rules", "membership"],
                "entities": [{"text": "5 books", "label": "CARDINAL", "start": 23, "end": 30}],
                "keywords": ["borrow", "books", "membership", "policy"],
                "word_count": 1250
            }
        }


class KnowledgeDocumentCreate(BaseModel):
    """Request model for creating a knowledge document."""
    title: str
    content: str
    doc_type: DocumentType = DocumentType.TXT
    source_url: Optional[str] = None
    category: Optional[str] = None
    tags: List[str] = Field(default_factory=list)
    author: Optional[str] = None


class KnowledgeDocumentUpdate(BaseModel):
    """Request model for updating a knowledge document."""
    title: Optional[str] = None
    content: Optional[str] = None
    category: Optional[str] = None
    tags: Optional[List[str]] = None


# ==================== KNOWLEDGE CHUNK ====================

class KnowledgeChunk(BaseModel):
    """A chunk of a document stored for vector search."""
    id: Optional[str] = None
    document_id: str = Field(..., description="Parent document ID")
    
    # Content
    text: str = Field(..., description="Chunk text content")
    chunk_index: int = Field(..., description="Position in document")
    
    # Embedding (stored separately but referenced here)
    embedding: Optional[List[float]] = None
    embedding_model: Optional[str] = None
    
    # Context
    start_char: int = 0
    end_char: int = 0
    surrounding_context: Optional[str] = None
    
    # Metadata inherited from document
    doc_type: DocumentType = DocumentType.TXT
    category: Optional[str] = None
    tags: List[str] = Field(default_factory=list)
    
    # Chunk-specific NLP
    entities: List[Dict[str, Any]] = Field(default_factory=list)
    keywords: List[str] = Field(default_factory=list)
    
    # Retrieval metrics
    retrieval_count: int = 0
    avg_relevance_score: Optional[float] = None
    
    created_at: datetime = Field(default_factory=datetime.utcnow)
    
    class Config:
        json_schema_extra = {
            "example": {
                "document_id": "doc_123",
                "text": "Members can borrow up to 5 books for 14 days.",
                "chunk_index": 0,
                "start_char": 0,
                "end_char": 50,
                "entities": [{"text": "5 books", "label": "CARDINAL"}],
                "keywords": ["borrow", "books", "14 days"]
            }
        }


# ==================== REFLECTION EDGE ====================

class ReflectionEdge(BaseModel):
    """
    A connection between two documents discovered by the knowledge reflection layer.
    Represents insights like 'Document A supports Document B' or 
    'Document C contradicts Document D'.
    """
    id: Optional[str] = None
    
    # Connected documents
    source_doc_id: str
    target_doc_id: str
    
    # Reflection metadata
    reflection_type: ReflectionType
    strength: float = Field(..., ge=0.0, le=1.0, description="Connection strength 0-1")
    
    # Linking details
    shared_entities: List[str] = Field(default_factory=list)
    shared_keywords: List[str] = Field(default_factory=list)
    semantic_similarity: Optional[float] = None
    
    # Synthesized insight
    insight_summary: Optional[str] = None
    insight_detail: Optional[str] = None
    
    # Evidence
    evidence_chunks: List[Dict[str, Any]] = Field(default_factory=list)
    
    # Discovery metadata
    discovered_by: str = "auto"  # auto, manual, admin
    discovery_method: str = "semantic+entity"
    
    # Validation
    is_validated: bool = False
    validated_by: Optional[str] = None
    validated_at: Optional[datetime] = None
    
    # Statistics
    usage_count: int = 0
    
    created_at: datetime = Field(default_factory=datetime.utcnow)
    updated_at: datetime = Field(default_factory=datetime.utcnow)
    
    class Config:
        json_schema_extra = {
            "example": {
                "source_doc_id": "doc_123",
                "target_doc_id": "doc_456",
                "reflection_type": "supporting",
                "strength": 0.85,
                "shared_entities": ["borrowing limit", "membership"],
                "semantic_similarity": 0.82,
                "insight_summary": "Both documents describe the 5-book borrowing limit",
                "insight_detail": "The 2024 policy (doc_123) extends the 2023 policy (doc_456) by adding e-book provisions..."
            }
        }


class ReflectionEdgeCreate(BaseModel):
    """Request model for creating a reflection edge."""
    source_doc_id: str
    target_doc_id: str
    reflection_type: ReflectionType
    strength: float = Field(..., ge=0.0, le=1.0)
    shared_entities: List[str] = Field(default_factory=list)
    shared_keywords: List[str] = Field(default_factory=list)
    semantic_similarity: Optional[float] = None
    insight_summary: Optional[str] = None
    insight_detail: Optional[str] = None


# ==================== CHAT / RAG INTERACTION ====================

class ChatMessage(BaseModel):
    """A single message in a chat session."""
    role: Literal["user", "assistant", "system"]
    content: str
    timestamp: datetime = Field(default_factory=datetime.utcnow)
    sources: Optional[List[Dict[str, Any]]] = None


class ChatRequest(BaseModel):
    """Request model for chat endpoint."""
    message: str = Field(..., description="User's question")
    session_id: Optional[str] = None
    member_id: Optional[str] = None
    
    # RAG configuration overrides
    top_k: Optional[int] = None
    include_sources: bool = True
    stream: bool = False
    
    # Context
    conversation_history: Optional[List[ChatMessage]] = None


class SourceCitation(BaseModel):
    """A citation for a source used in the answer."""
    document_id: str
    document_title: str
    chunk_text: str
    chunk_index: int
    relevance_score: float
    doc_type: DocumentType


class ChatResponse(BaseModel):
    """Response model for chat endpoint."""
    answer: str
    sources: List[SourceCitation] = Field(default_factory=list)
    session_id: str
    confidence_score: Optional[float] = None
    
    # RAG metadata
    retrieval_time_ms: Optional[int] = None
    generation_time_ms: Optional[int] = None
    total_time_ms: Optional[int] = None
    
    # Knowledge reflection used
    reflections_used: Optional[List[Dict[str, Any]]] = None
    
    # For self-learning
    response_id: str
    created_at: datetime = Field(default_factory=datetime.utcnow)


class ChatFeedbackRequest(BaseModel):
    """Request model for submitting chat feedback."""
    response_id: str
    feedback_type: FeedbackType
    comment: Optional[str] = None
    member_id: Optional[str] = None


# ==================== RAG EVALUATION ====================

class RagEvaluation(BaseModel):
    """Evaluation metrics for a RAG response."""
    id: Optional[str] = None
    response_id: str
    
    # Input
    query: str
    retrieved_chunks: List[str] = Field(default_factory=list)
    generated_answer: str
    
    # RAGAS-style metrics (0-1)
    faithfulness: Optional[float] = None
    answer_relevancy: Optional[float] = None
    context_precision: Optional[float] = None
    context_recall: Optional[float] = None
    context_entity_recall: Optional[float] = None
    answer_similarity: Optional[float] = None
    answer_correctness: Optional[float] = None
    
    # Overall score
    overall_score: Optional[float] = None
    
    # Feedback
    human_rating: Optional[int] = None
    feedback_type: Optional[FeedbackType] = None
    
    # For active learning
    confidence_score: Optional[float] = None
    is_low_confidence: bool = False
    needs_human_review: bool = False
    
    created_at: datetime = Field(default_factory=datetime.utcnow)


class InsightSynthesis(BaseModel):
    """A synthesized insight across multiple documents."""
    id: Optional[str] = None
    
    # Source documents
    document_ids: List[str]
    
    # Synthesis
    title: str
    summary: str
    key_points: List[str] = Field(default_factory=list)
    
    # Type
    synthesis_type: Literal["comparison", "contrast", "timeline", "consensus", "conflict", "extension"]
    
    # Confidence
    confidence: float = Field(..., ge=0.0, le=1.0)
    
    # Evidence
    supporting_edges: List[str] = Field(default_factory=list)
    
    created_at: datetime = Field(default_factory=datetime.utcnow)
    updated_at: datetime = Field(default_factory=datetime.utcnow)


# ==================== SEARCH / RETRIEVAL ====================

class SearchResult(BaseModel):
    """A single search result from vector search."""
    chunk_id: str
    document_id: str
    document_title: str
    text: str
    score: float
    metadata: Dict[str, Any] = Field(default_factory=dict)


class KnowledgeSearchRequest(BaseModel):
    """Request for searching the knowledge base."""
    query: str
    top_k: int = 10
    doc_types: Optional[List[DocumentType]] = None
    categories: Optional[List[str]] = None
    tags: Optional[List[str]] = None
    include_reflections: bool = True


class KnowledgeSearchResponse(BaseModel):
    """Response for knowledge base search."""
    results: List[SearchResult]
    total_results: int
    query_time_ms: int
    
    # Related insights
    related_reflections: List[ReflectionEdge] = Field(default_factory=list)
    synthesized_insights: List[InsightSynthesis] = Field(default_factory=list)


# ==================== ADMIN / MANAGEMENT ====================

class IngestionJob(BaseModel):
    """A document ingestion job."""
    id: Optional[str] = None
    status: Literal["queued", "processing", "completed", "failed"]
    files_count: int
    processed_count: int = 0
    failed_count: int = 0
    errors: List[str] = Field(default_factory=list)
    created_at: datetime = Field(default_factory=datetime.utcnow)
    completed_at: Optional[datetime] = None


class KnowledgeStats(BaseModel):
    """Statistics for the knowledge base."""
    total_documents: int
    total_chunks: int
    total_reflections: int
    total_synthesized_insights: int
    
    documents_by_type: Dict[str, int] = Field(default_factory=dict)
    documents_by_status: Dict[str, int] = Field(default_factory=dict)
    
    # Activity
    documents_added_7d: int
    documents_added_30d: int
    queries_7d: int
    queries_30d: int
    
    # Quality
    avg_feedback_score: Optional[float] = None
    low_confidence_queries_7d: int
    
    updated_at: datetime = Field(default_factory=datetime.utcnow)
