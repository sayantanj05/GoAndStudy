"""FastAPI service for GoAndStudy data mining reports and jobs."""

from __future__ import annotations

import csv
import subprocess
import sys
from pathlib import Path
from typing import Any

from fastapi import FastAPI, HTTPException, Query, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import PlainTextResponse, JSONResponse

ROOT = Path(__file__).resolve().parents[1]

# Import ETL service
try:
    from .etl_service import get_etl_service, RealtimeETLService
    ETL_AVAILABLE = True
except ImportError:
    ETL_AVAILABLE = False
    print("Warning: ETL service not available")
REPORTS_DIR = ROOT / "reports"
SCRIPTS_DIR = ROOT / "scripts"

PHASE_JOBS = {
    "descriptive": "phase2_1_descriptive_analytics.py",
    "correlation": "phase2_2_correlation_analysis.py",
    "book-similarity": "phase2_3_book_similarity.py",
    "clusters": "phase3_1_kmeans_clustering.py",
    "association-rules": "phase3_2_apriori_rules.py",
    "forecast": "phase3_3_prophet_forecast.py",
    "anomalies": "phase3_4_isolation_forest.py",
    "churn": "phase4_1_churn_prediction.py",
    "overdue": "phase4_2_overdue_prediction.py",
    "fine": "phase4_3_fine_estimation.py",
}

app = FastAPI(
    title="GoAndStudy Data Mining Service",
    description="Analytics, reports, and batch data-mining job runner.",
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


def read_csv(path: Path, limit: int = 100) -> list[dict[str, Any]]:
    if not path.exists():
        raise HTTPException(status_code=404, detail=f"Report not found: {path.name}")
    with path.open(newline="", encoding="utf-8-sig") as handle:
        rows = list(csv.DictReader(handle))
    return rows[:limit]


def read_text_report(relative_path: str) -> str:
    path = (REPORTS_DIR / relative_path).resolve()
    if REPORTS_DIR.resolve() not in path.parents:
        raise HTTPException(status_code=400, detail="Invalid report path")
    if not path.exists() or path.suffix.lower() not in {".md", ".txt", ".csv"}:
        raise HTTPException(status_code=404, detail="Report not found")
    return path.read_text(encoding="utf-8-sig")


@app.get("/")
def root() -> dict[str, Any]:
    return {
        "service": "GoAndStudy Data Mining Service",
        "status": "UP",
        "health": "/health",
        "reports": "/reports",
        "jobs": sorted(PHASE_JOBS),
    }


@app.get("/health")
def health() -> dict[str, Any]:
    health_data = {
        "status": "healthy",
        "reports_dir": str(REPORTS_DIR),
        "scripts_dir": str(SCRIPTS_DIR),
        "available_jobs": sorted(PHASE_JOBS),
        "etl_available": ETL_AVAILABLE,
    }
    
    # Add ETL status if available
    if ETL_AVAILABLE:
        etl = get_etl_service()
        health_data["etl_running"] = etl.running if hasattr(etl, 'running') else False
    
    return health_data


@app.get("/reports")
def list_reports() -> dict[str, Any]:
    files = []
    for path in sorted(REPORTS_DIR.rglob("*")):
        if path.is_file() and path.suffix.lower() in {".md", ".csv", ".txt"}:
            files.append(str(path.relative_to(REPORTS_DIR)).replace("\\", "/"))
    return {"count": len(files), "reports": files}


@app.get("/reports/{report_path:path}", response_class=PlainTextResponse)
def get_report(report_path: str) -> str:
    return read_text_report(report_path)


@app.get("/clusters")
def member_clusters(limit: int = Query(100, ge=1, le=1000)) -> dict[str, Any]:
    rows = read_csv(REPORTS_DIR / "phase3_1" / "member_clusters.csv", limit)
    return {"count": len(rows), "items": rows}


@app.get("/association-rules")
def association_rules(limit: int = Query(100, ge=1, le=1000)) -> dict[str, Any]:
    rows = read_csv(REPORTS_DIR / "phase3_2" / "association_rules.csv", limit)
    return {"count": len(rows), "items": rows}


@app.get("/forecast/loans")
def loan_forecast(limit: int = Query(30, ge=1, le=365)) -> dict[str, Any]:
    rows = read_csv(REPORTS_DIR / "phase3_3" / "loan_forecast.csv", limit)
    return {"count": len(rows), "items": rows}


@app.get("/anomalies")
def anomalies(limit: int = Query(100, ge=1, le=1000)) -> dict[str, Any]:
    rows = read_csv(REPORTS_DIR / "phase3_4" / "anomaly_detection.csv", limit)
    return {"count": len(rows), "items": rows}


@app.get("/models/churn")
def churn_predictions(limit: int = Query(100, ge=1, le=1000)) -> dict[str, Any]:
    rows = read_csv(ROOT / "scripts" / "data_mining" / "reports" / "phase4_1" / "churn_prediction.csv", limit)
    return {"count": len(rows), "items": rows}


@app.get("/models/overdue")
def overdue_predictions(limit: int = Query(100, ge=1, le=1000)) -> dict[str, Any]:
    rows = read_csv(ROOT / "scripts" / "data_mining" / "reports" / "phase4_2" / "overdue_prediction.csv", limit)
    return {"count": len(rows), "items": rows}


@app.get("/models/fine")
def fine_estimations(limit: int = Query(100, ge=1, le=1000)) -> dict[str, Any]:
    rows = read_csv(ROOT / "scripts" / "data_mining" / "reports" / "phase4_3" / "fine_estimation.csv", limit)
    return {"count": len(rows), "items": rows}


@app.post("/jobs/{job_name}/run")
def run_job(job_name: str) -> dict[str, Any]:
    script = PHASE_JOBS.get(job_name)
    if script is None:
        raise HTTPException(status_code=404, detail=f"Unknown job: {job_name}")

    result = subprocess.run(
        [sys.executable, str(SCRIPTS_DIR / script)],
        cwd=str(ROOT.parent),
        capture_output=True,
        text=True,
        timeout=300,
    )
    return {
        "job": job_name,
        "script": script,
        "returncode": result.returncode,
        "stdout": result.stdout[-8000:],
        "stderr": result.stderr[-8000:],
        "ok": result.returncode == 0,
    }


# ETL Endpoints
@app.post("/etl/start")
def etl_start() -> dict[str, Any]:
    """Start the real-time ETL service"""
    if not ETL_AVAILABLE:
        raise HTTPException(status_code=503, detail="ETL service not available")
    
    etl = get_etl_service()
    if etl.running:
        return {"status": "already_running", "message": "ETL service is already running"}
    
    success = etl.start()
    if success:
        return {"status": "started", "message": "Real-time ETL service started successfully"}
    else:
        raise HTTPException(status_code=500, detail="Failed to start ETL service")


@app.post("/etl/stop")
def etl_stop() -> dict[str, Any]:
    """Stop the real-time ETL service"""
    if not ETL_AVAILABLE:
        raise HTTPException(status_code=503, detail="ETL service not available")
    
    etl = get_etl_service()
    if not etl.running:
        return {"status": "not_running", "message": "ETL service is not running"}
    
    etl.stop()
    return {"status": "stopped", "message": "ETL service stopped"}


@app.get("/etl/status")
def etl_status() -> dict[str, Any]:
    """Get ETL service status and statistics"""
    if not ETL_AVAILABLE:
        raise HTTPException(status_code=503, detail="ETL service not available")
    
    etl = get_etl_service()
    return etl.get_stats()


@app.post("/etl/sync")
def etl_force_sync() -> dict[str, Any]:
    """Force immediate sync of all collections"""
    if not ETL_AVAILABLE:
        raise HTTPException(status_code=503, detail="ETL service not available")
    
    etl = get_etl_service()
    if not etl.running:
        # Try to connect and sync once
        if not etl.connect():
            raise HTTPException(status_code=503, detail="Cannot connect to databases")
    
    results = etl.force_sync_all()
    return {"status": "synced", "results": results}
