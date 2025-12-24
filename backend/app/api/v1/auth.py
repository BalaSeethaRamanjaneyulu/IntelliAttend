"""
Authentication API routes
"""
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.db.database import get_db
from app.schemas.auth_schema import LoginRequest, TokenResponse, TokenRefreshRequest, UserProfile, FirebaseLoginRequest
from app.services.auth_service import AuthService

router = APIRouter()


@router.post("/login", response_model=TokenResponse)
async def login(
    login_data: LoginRequest,
    db: Session = Depends(get_db)
):
    """
    Authenticate user (student or faculty) and return JWT tokens
    
    - **username**: student_id or faculty_id
    - **password**: user password
    - **role**: "student" or "faculty"
    """
    return AuthService.authenticate_user(db, login_data)


@router.post("/login/firebase", response_model=TokenResponse)
async def login_firebase(
    login_data: FirebaseLoginRequest,
    db: Session = Depends(get_db)
):
    """
    Authenticate user using Firebase Phone Auth token
    """
    return AuthService.authenticate_with_firebase(db, login_data)


@router.post("/refresh", response_model=TokenResponse)
async def refresh_token(
    refresh_data: TokenRefreshRequest,
    db: Session = Depends(get_db)
):
    """
    Refresh access token using refresh token
    """
    return AuthService.refresh_access_token(db, refresh_data.refresh_token)


@router.get("/me", response_model=UserProfile)
async def get_current_user_info(
    token: str,
    db: Session = Depends(get_db)
):
    """
    Get current user profile from JWT token
    
    Pass token as query parameter: /api/v1/auth/me?token=<jwt_token>
    """
    return AuthService.get_current_user(db, token)
