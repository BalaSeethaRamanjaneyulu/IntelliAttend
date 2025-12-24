"""
Schedule Service - Fetch faculty schedules from Firestore
"""
from fastapi import HTTPException, status
import firebase_admin
from firebase_admin import firestore
from app.core import firebase
from datetime import datetime, timedelta
from typing import List, Dict

class ScheduleService:
    """Handle schedule queries and validation"""
    
    @staticmethod
    def _get_db():
        """Get Firestore client"""
        if not firebase_admin._apps:
            firebase.initialize_firebase()
        return firestore.client()
    
    @staticmethod
    def _get_day_name(date_obj: datetime) -> str:
        """Get day name from date"""
        days = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday']
        return days[date_obj.weekday()]
    
    @staticmethod
    def _calculate_status(slot_start: str, slot_end: str, current_time: str) -> str:
        """Calculate if class is completed, current, or upcoming"""
        if current_time > slot_end:
            return "completed"
        elif current_time >= slot_start:
            return "current"
        else:
            return "upcoming"
    
    @staticmethod
    def _can_start_session(slot_start: str, current_time: str) -> bool:
        """Check if session can be started (5 min before to 15 min after)"""
        # Parse times (format: "HH:MM")
        start_hour, start_min = map(int, slot_start.split(':'))
        current_hour, current_min = map(int, current_time.split(':'))
        
        # Convert to minutes
        start_minutes = start_hour * 60 + start_min
        current_minutes = current_hour * 60 + current_min
        
        # Check if within window (-5 to +15 minutes)
        diff = current_minutes - start_minutes
        return -5 <= diff <= 15
    
    @staticmethod
    async def get_faculty_schedule(faculty_id: str, date: str = None):
        """
        Get faculty schedule for a specific date
        
        Args:
            faculty_id: Faculty ID
            date: Date in YYYY-MM-DD format (defaults to today)
        """
        db = ScheduleService._get_db()
        
        # Parse date or use today
        if date:
            target_date = datetime.strptime(date, '%Y-%m-%d')
        else:
            target_date = datetime.now()
        
        day_name = ScheduleService._get_day_name(target_date)
        current_time = target_date.strftime('%H:%M')
        
        # Query timetable slots for this faculty and day
        slots_ref = db.collection('timetable_slots') \
            .where('faculty_id', '==', faculty_id) \
            .where('day', '==', day_name)
        
        slots = slots_ref.stream()
        
        schedule = []
        for slot_doc in slots:
            slot_data = slot_doc.to_dict()
            
            # Get subject details
            subject_ref = db.collection('subjects').document(slot_data['subject_id'])
            subject = subject_ref.get()
            subject_data = subject.to_dict() if subject.exists else {}
            
            # Get classroom details
            classroom_ref = db.collection('classrooms').document(slot_data['classroom_id'])
            classroom = classroom_ref.get()
            classroom_data = classroom.to_dict() if classroom.exists else {}
            
            # Get section details
            section_ref = db.collection('sections').document(slot_data['section_id'])
            section = section_ref.get()
            section_data = section.to_dict() if section.exists else {}
            
            # Calculate status
            slot_status = ScheduleService._calculate_status(
                slot_data['start_time'],
                slot_data['end_time'],
                current_time
            )
            
            # Check if can start
            can_start = ScheduleService._can_start_session(
                slot_data['start_time'],
                current_time
            )
            
            schedule.append({
                'slot_id': slot_doc.id,
                'slot_number': slot_data.get('slot_number'),
                'subject_id': slot_data['subject_id'],
                'subject_name': subject_data.get('name', 'Unknown'),
                'subject_code': subject_data.get('code', ''),
                'subject_short': subject_data.get('short_name', ''),
                'section_id': slot_data['section_id'],
                'section_name': section_data.get('section_name', ''),
                'classroom_id': slot_data['classroom_id'],
                'room_number': classroom_data.get('room_number', ''),
                'start_time': slot_data['start_time'],
                'end_time': slot_data['end_time'],
                'is_lab': slot_data.get('is_lab', False),
                'status': slot_status,
                'can_start': can_start and slot_status != 'completed'
            })
        
        # Sort by start time
        schedule.sort(key=lambda x: x['start_time'])
        
        return {
            'date': target_date.strftime('%Y-%m-%d'),
            'day': day_name,
            'current_time': current_time,
            'total_classes': len(schedule),
            'classes': schedule
        }
    
    @staticmethod
    async def get_current_class(faculty_id: str):
        """Get faculty's current class (if any)"""
        schedule = await ScheduleService.get_faculty_schedule(faculty_id)
        
        # Find current class
        current_classes = [c for c in schedule['classes'] if c['status'] == 'current']
        
        if current_classes:
            return current_classes[0]
        
        # Find next upcoming class
        upcoming_classes = [c for c in schedule['classes'] if c['status'] == 'upcoming']
        if upcoming_classes:
            return {
                **upcoming_classes[0],
                'is_upcoming': True
            }
        
        return None
    
    @staticmethod
    async def validate_session_start(faculty_id: str, slot_id: str) -> Dict:
        """
        Validate if faculty can start a session
        
        Returns slot details if valid, raises exception otherwise
        """
        db = ScheduleService._get_db()
        
        # Get slot
        slot_ref = db.collection('timetable_slots').document(slot_id)
        slot_doc = slot_ref.get()
        
        if not slot_doc.exists:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Timetable slot not found"
            )
        
        slot_data = slot_doc.to_dict()
        
        # Verify faculty owns this slot
        if slot_data['faculty_id'] != faculty_id:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="This class is not assigned to you"
            )
        
        # Check day
        current_day = ScheduleService._get_day_name(datetime.now())
        if slot_data['day'] != current_day:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"This class is scheduled for {slot_data['day']}, not {current_day}"
            )
        
        # Check time window
        current_time = datetime.now().strftime('%H:%M')
        if not ScheduleService._can_start_session(slot_data['start_time'], current_time):
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Session can only be started 5 minutes before to 15 minutes after scheduled time"
            )
        
        # Check for duplicate active sessions
        active_sessions = db.collection('sessions') \
            .where('faculty_id', '==', faculty_id) \
            .where('status', '==', 'active') \
            .limit(1) \
            .stream()
        
        for session in active_sessions:
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT,
                detail="You already have an active session. Please end it before starting a new one."
            )
        
        return {
            'slot_id': slot_id,
            'slot_data': slot_data,
            'valid': True
        }
