"""
Analytics API routes
"""
from fastapi import APIRouter
from app.services.analytics_service import AnalyticsService

router = APIRouter()

@router.get("/faculty/{faculty_id}/summary")
async def get_faculty_summary(faculty_id: str, days: int = 30):
    """
    Get faculty teaching summary
    
    Returns:
    - Total sessions conducted
    - Total students taught
    - Subjects taught
    - Faculty attendance rate
    - Average students per session
    """
    return await AnalyticsService.get_faculty_summary(faculty_id, days)


@router.get("/session/{session_id}/report")
async def get_session_report(session_id: str):
    """
    Get detailed session report
    
    Returns:
    - Session details
    - Attendance statistics
    - List of students with verification data
    """
    return await AnalyticsService.get_session_report(session_id)


@router.get("/subject/{subject_id}/attendance")
async def get_subject_attendance(subject_id: str, section_id: str = None, days: int = 30):
    """
    Get subject-wise attendance trends
    
    Returns:
    - Overall attendance percentage
    - Session-wise breakdown
    - Trends over time
    """
    return await AnalyticsService.get_subject_attendance(subject_id, section_id, days)


@router.get("/section/{section_id}/trends")
async def get_section_trends(section_id: str, days: int = 30):
    """
    Get section attendance trends
    
    Returns:
    - Subject-wise attendance
    - Daily trends
    - Overall statistics
    """
    return await AnalyticsService.get_section_trends(section_id, days)
