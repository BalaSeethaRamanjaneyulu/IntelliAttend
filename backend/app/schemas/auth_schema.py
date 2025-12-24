"""
Pydantic schemas for authentication endpoints
"""
from pydantic import BaseModel, EmailStr
from typing import Optional


class LoginRequest(BaseModel):
    """Login credentials"""
    username: str  # Can be student_id or faculty_id
    password: str
    role: str  # "student" or "faculty"


class FirebaseLoginRequest(BaseModel):
    """Firebase Login request"""
    token: str
    role: Optional[str] = "student" # Default to student if not specified, but should be handled by logic


class TokenResponse(BaseModel):
    """JWT token response"""
    access_token: str
    refresh_token: str
    token_type: str = "bearer"
    expires_in: int  # seconds


class TokenRefreshRequest(BaseModel):
    """Refresh token request"""
    refresh_token: str


class UserProfile(BaseModel):
    """Basic user profile"""
    id: str # Changed from int to str for Firestore IDs
    user_id: str  # student_id or faculty_id
    name: str
    email: str
    role: str
    
    class Config:
        from_attributes = True
