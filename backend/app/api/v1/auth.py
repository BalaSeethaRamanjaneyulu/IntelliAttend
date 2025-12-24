"""
Authentication API routes
"""
from fastapi import APIRouter, Depends, HTTPException

from app.schemas.auth_schema import LoginRequest, TokenResponse, TokenRefreshRequest, UserProfile, FirebaseLoginRequest
from app.services.auth_service import AuthService

router = APIRouter()

@router.post("/login", response_model=TokenResponse)
async def login(login_data: LoginRequest):
    """
    Authenticate user (student or faculty) and return JWT tokens
    """
    # Pass None for db_session as it's no longer used
    return AuthService.authenticate_user(None, login_data)


@router.post("/login/firebase", response_model=TokenResponse)
async def login_firebase(login_data: FirebaseLoginRequest):
    """
    Authenticate user using Firebase Phone Auth token
    """
    return AuthService.authenticate_with_firebase(None, login_data)


@router.post("/refresh", response_model=TokenResponse)
async def refresh_token(refresh_data: TokenRefreshRequest):
    """
    Refresh access token using refresh token
    """
    # Note: refresh logic likely needs minor update in service too, but prioritizing login
    # return AuthService.refresh_access_token(None, refresh_data.refresh_token)
    pass 


@router.get("/me", response_model=UserProfile)
async def get_current_user_info(token: str):
    """
    Get current user profile from JWT token
    Pass token as query parameter: /api/v1/auth/me?token=<jwt_token>
    """
    return AuthService.get_current_user(None, token)
