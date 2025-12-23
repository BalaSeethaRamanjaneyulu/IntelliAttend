"""
Authentication service for JWT and user verification
"""
from sqlalchemy.orm import Session
from fastapi import HTTPException, status
from datetime import timedelta

from app.models.student import Student
from app.models.faculty import Faculty
from app.core.security import (
    verify_password,
    get_password_hash,
    create_access_token,
    create_refresh_token,
    decode_token
)
from app.core.config import settings
from app.schemas.auth_schema import LoginRequest, TokenResponse, UserProfile


class AuthService:
    """Handle authentication and token management"""
    
    @staticmethod
    def authenticate_user(db: Session, login_data: LoginRequest) -> TokenResponse:
        """
        Authenticate user and return JWT tokens
        
        Args:
            db: Database session
            login_data: Login credentials (username, password, role)
            
        Returns:
            TokenResponse with access and refresh tokens
            
        Raises:
            HTTPException: if credentials are invalid
        """
        user = None
        user_id = None
        
        # Authenticate based on role
        if login_data.role == "student":
            user = db.query(Student).filter(
                Student.student_id == login_data.username
            ).first()
            user_id = user.student_id if user else None
            
        elif login_data.role == "faculty":
            user = db.query(Faculty).filter(
                Faculty.faculty_id == login_data.username
            ).first()
            user_id = user.faculty_id if user else None
        else:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Invalid role. Must be 'student' or 'faculty'"
            )
        
        # Verify user exists and password is correct
        if not user or not verify_password(login_data.password, user.password_hash):
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid username or password",
                headers={"WWW-Authenticate": "Bearer"},
            )
        
        # Create tokens
        token_data = {
            "sub": user_id,
            "role": login_data.role,
            "id": user.id
        }
        
        access_token = create_access_token(token_data)
        refresh_token = create_refresh_token(token_data)
        
        return TokenResponse(
            access_token=access_token,
            refresh_token=refresh_token,
            token_type="bearer",
            expires_in=settings.JWT_EXPIRES_IN
        )
    
    @staticmethod
    def refresh_access_token(db: Session, refresh_token: str) -> TokenResponse:
        """
        Generate new access token from refresh token
        
        Args:
            db: Database session
            refresh_token: Valid refresh token
            
        Returns:
            TokenResponse with new access and refresh tokens
            
        Raises:
            HTTPException: if refresh token is invalid
        """
        try:
            payload = decode_token(refresh_token)
            
            # Verify it's a refresh token
            if payload.get("type") != "refresh":
                raise HTTPException(
                    status_code=status.HTTP_401_UNAUTHORIZED,
                    detail="Invalid refresh token"
                )
            
            # Create new tokens
            token_data = {
                "sub": payload.get("sub"),
                "role": payload.get("role"),
                "id": payload.get("id")
            }
            
            new_access_token = create_access_token(token_data)
            new_refresh_token = create_refresh_token(token_data)
            
            return TokenResponse(
                access_token=new_access_token,
                refresh_token=new_refresh_token,
                token_type="bearer",
                expires_in=settings.JWT_EXPIRES_IN
            )
            
        except Exception as e:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid or expired refresh token"
            )
    
    @staticmethod
    def get_current_user(db: Session, token: str) -> UserProfile:
        """
        Get current user from JWT token
        
        Args:
            db: Database session
            token: JWT access token
            
        Returns:
            UserProfile object
            
        Raises:
            HTTPException: if token is invalid
        """
        try:
            payload = decode_token(token)
            user_id = payload.get("sub")
            role = payload.get("role")
            
            if not user_id or not role:
                raise HTTPException(
                    status_code=status.HTTP_401_UNAUTHORIZED,
                    detail="Invalid token payload"
                )
            
            # Get user from database
            if role == "student":
                user = db.query(Student).filter(Student.student_id == user_id).first()
            else:
                user = db.query(Faculty).filter(Faculty.faculty_id == user_id).first()
            
            if not user:
                raise HTTPException(
                    status_code=status.HTTP_404_NOT_FOUND,
                    detail="User not found"
                )
            
            return UserProfile(
                id=user.id,
                user_id=user_id,
                name=user.name,
                email=user.email,
                role=role
            )
            
        except Exception as e:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Could not validate credentials"
            )
