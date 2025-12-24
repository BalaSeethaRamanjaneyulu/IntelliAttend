"""
Schedule API routes
"""
from fastapi import APIRouter, HTTPException
from app.services.schedule_service import ScheduleService

router = APIRouter()

@router.get("/faculty/{faculty_id}/schedule")
async def get_faculty_schedule(faculty_id: str, date: str = None):
    """
    Get faculty schedule for a specific date
    
    Args:
        faculty_id: Faculty ID
        date: Optional date in YYYY-MM-DD format (defaults to today)
    
    Returns:
        Schedule with all classes for the day
    """
    return await ScheduleService.get_faculty_schedule(faculty_id, date)


@router.get("/faculty/{faculty_id}/schedule/current")
async def get_current_class(faculty_id: str):
    """
    Get faculty's current class (if any)
    
    Returns current class or next upcoming class
    """
    current_class = await ScheduleService.get_current_class(faculty_id)
    
    if not current_class:
        return {
            'has_class': False,
            'message': 'No classes scheduled for today'
        }
    
    return {
        'has_class': True,
        'class': current_class
    }
