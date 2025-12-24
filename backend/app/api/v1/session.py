"""
Session API routes
"""
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from app.services.session_service_firestore import SessionService

router = APIRouter()

class OTPVerifyRequest(BaseModel):
    otp: str

class SessionCreateRequest(BaseModel):
    faculty_id: str
    course_id: str
    slot_id: str | None = None
    device_id: str | None = None
    biometric_verified: bool = False

@router.post("/verify-otp")
async def verify_otp(request: OTPVerifyRequest):
    """
    Verify OTP entered on SmartBoard
    
    This endpoint:
    1. Validates the OTP against active sessions
    2. Marks faculty as PRESENT (attendance logging)
    3. Authorizes SmartBoard to display QR code
    """
    return await SessionService.verify_otp_and_mark_faculty(request.otp)


@router.post("/create")
async def create_session(request: SessionCreateRequest):
    """
    Create a new attendance session
    
    Faculty calls this from mobile app to start a session.
    Returns OTP that faculty must enter on SmartBoard.
    
    If slot_id is provided, validates schedule and time window.
    """
    return await SessionService.create_session(
        request.faculty_id,
        request.course_id,
        request.slot_id,
        request.device_id,
        request.biometric_verified
    )


@router.get("/{session_id}")
async def get_session(session_id: str):
    """Get session details"""
    return await SessionService.get_session_details(session_id)


@router.post("/{session_id}/end")
async def end_session(session_id: str):
    """End an active session"""
    return await SessionService.end_session(session_id)
