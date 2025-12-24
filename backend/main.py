"""
IntelliAttend - Main FastAPI Application Entry Point
"""
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager

from app.core.config import settings
# Database imports removed - using Firestore only
from app.api.v1 import auth, session, schedule, attendance, websocket, analytics  # Migrated to Firestore
# TODO: Migrate these routers to Firestore
# from app.api.v1 import student, faculty


@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Lifecycle events for FastAPI app
    """
    # Startup
    print("🚀 Starting IntelliAttend API Server...")
    print(f"🔥 Database: Cloud Firestore ({settings.FIREBASE_PROJECT_ID})")
    
    # Start token rotation scheduler
    from app.services.token_rotation_scheduler import start_token_rotation
    start_token_rotation()
    
    yield
    
    # Shutdown
    print("👋 Shutting down IntelliAttend API Server...")
    
    # Stop token rotation scheduler
    from app.services.token_rotation_scheduler import stop_token_rotation
    stop_token_rotation()


# Initialize FastAPI app
app = FastAPI(
    title="IntelliAttend API",
    description="Attendance Management System with Firestore",
    version="2.0.0",
    lifespan=lifespan
)

# CORS Configuration - Development Mode (Allow All Origins)
# TODO: Restrict this in production to specific origins only
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Temporarily allow all origins for testing
    allow_credentials=False,  # Must be False when allow_origins=["*"]
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include API routers (only migrated ones)
app.include_router(auth.router, prefix="/api/v1/auth", tags=["Authentication"])
app.include_router(session.router, prefix="/api/v1/sessions", tags=["Sessions"])
app.include_router(schedule.router, prefix="/api/v1/schedule", tags=["Schedule"])
app.include_router(attendance.router, prefix="/api/v1/attendance", tags=["Attendance"])
app.include_router(websocket.router, prefix="/api/v1/websocket", tags=["WebSocket"])
app.include_router(analytics.router, prefix="/api/v1/analytics", tags=["Analytics"])

# Validation logging endpoint (for mobile app debugging)
from app.api.v1 import validation_log
app.include_router(validation_log.router, prefix="/api/v1/attendance", tags=["Validation"])

# TODO: Re-enable after migration
# app.include_router(student.router, prefix="/api/v1/student", tags=["Student"])
# app.include_router(faculty.router, prefix="/api/v1/faculty", tags=["Faculty"])


@app.get("/", tags=["Root"])
async def root():
    """Root endpoint"""
    return {
        "message": "Welcome to IntelliAttend API",
        "version": "1.0.0",
        "docs": "/docs"
    }


@app.get("/health", tags=["Health"])
async def health_check():
    """Health check endpoint"""
    return {
        "status": "ok",
        "service": "IntelliAttend API",
        "version": "1.0.0"
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "main:app",
        host=settings.HOST,
        port=settings.PORT,
        reload=settings.DEBUG
    )
