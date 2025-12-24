"""
Analytics Service - Generate attendance reports and statistics
"""
import firebase_admin
from firebase_admin import firestore
from app.core import firebase
from datetime import datetime, timedelta
from typing import Dict, List
from collections import defaultdict

class AnalyticsService:
    """Handle analytics and reporting"""
    
    @staticmethod
    def _get_db():
        """Get Firestore client"""
        if not firebase_admin._apps:
            firebase.initialize_firebase()
        return firestore.client()
    
    @staticmethod
    async def get_faculty_summary(faculty_id: str, days: int = 30):
        """
        Get faculty teaching summary
        
        Args:
            faculty_id: Faculty ID
            days: Number of days to look back
        
        Returns:
            Summary statistics
        """
        db = AnalyticsService._get_db()
        
        # Get all sessions by this faculty
        cutoff_date = datetime.now() - timedelta(days=days)
        
        sessions = db.collection('sessions') \
            .where('faculty_id', '==', faculty_id) \
            .where('created_at', '>=', cutoff_date) \
            .stream()
        
        total_sessions = 0
        total_students_present = 0
        subjects = set()
        
        for session_doc in sessions:
            total_sessions += 1
            session_data = session_doc.to_dict()
            subjects.add(session_data.get('subject_id'))
            
            # Count attendance for this session
            attendance_count = db.collection('student_attendance') \
                .where('session_id', '==', session_doc.id) \
                .where('status', '==', 'present') \
                .stream()
            
            total_students_present += sum(1 for _ in attendance_count)
        
        # Get faculty attendance
        faculty_attendance = db.collection('faculty_attendance') \
            .where('faculty_id', '==', faculty_id) \
            .where('timestamp', '>=', cutoff_date) \
            .stream()
        
        faculty_present_count = sum(1 for _ in faculty_attendance)
        
        return {
            'faculty_id': faculty_id,
            'period_days': days,
            'total_sessions': total_sessions,
            'total_students_taught': total_students_present,
            'unique_subjects': len(subjects),
            'faculty_attendance_rate': round((faculty_present_count / total_sessions * 100) if total_sessions > 0 else 0, 2),
            'avg_students_per_session': round(total_students_present / total_sessions, 2) if total_sessions > 0 else 0
        }
    
    @staticmethod
    async def get_session_report(session_id: str):
        """
        Get detailed report for a specific session
        
        Args:
            session_id: Session ID
        
        Returns:
            Session report with attendance details
        """
        db = AnalyticsService._get_db()
        
        # Get session
        session_ref = db.collection('sessions').document(session_id)
        session_doc = session_ref.get()
        
        if not session_doc.exists:
            return {'error': 'Session not found'}
        
        session_data = session_doc.to_dict()
        
        # Get all attendance records
        attendance_records = db.collection('student_attendance') \
            .where('session_id', '==', session_id) \
            .stream()
        
        present_count = 0
        suspicious_count = 0
        students = []
        
        for att_doc in attendance_records:
            att_data = att_doc.to_dict()
            
            if att_data.get('status') == 'present':
                present_count += 1
            elif att_data.get('status') == 'suspicious':
                suspicious_count += 1
            
            students.append({
                'student_id': att_data.get('student_id'),
                'status': att_data.get('status'),
                'timestamp': att_data.get('timestamp'),
                'verification': att_data.get('verification_data', {})
            })
        
        # Get expected student count from section
        expected_count = 0
        if session_data.get('section_id'):
            section_ref = db.collection('sections').document(session_data['section_id'])
            section_doc = section_ref.get()
            if section_doc.exists:
                expected_count = section_doc.to_dict().get('total_students', 0)
        
        attendance_percentage = round((present_count / expected_count * 100) if expected_count > 0 else 0, 2)
        
        return {
            'session_id': session_id,
            'subject_id': session_data.get('subject_id'),
            'section_id': session_data.get('section_id'),
            'faculty_id': session_data.get('faculty_id'),
            'classroom_id': session_data.get('classroom_id'),
            'created_at': session_data.get('created_at'),
            'status': session_data.get('status'),
            'statistics': {
                'expected_students': expected_count,
                'present': present_count,
                'absent': expected_count - present_count,
                'suspicious': suspicious_count,
                'attendance_percentage': attendance_percentage
            },
            'students': students
        }
    
    @staticmethod
    async def get_subject_attendance(subject_id: str, section_id: str = None, days: int = 30):
        """
        Get attendance trends for a subject
        
        Args:
            subject_id: Subject ID
            section_id: Optional section filter
            days: Number of days to look back
        
        Returns:
            Attendance trends
        """
        db = AnalyticsService._get_db()
        
        cutoff_date = datetime.now() - timedelta(days=days)
        
        # Get all sessions for this subject
        query = db.collection('sessions') \
            .where('subject_id', '==', subject_id) \
            .where('created_at', '>=', cutoff_date)
        
        if section_id:
            query = query.where('section_id', '==', section_id)
        
        sessions = query.stream()
        
        session_stats = []
        total_present = 0
        total_expected = 0
        
        for session_doc in sessions:
            session_data = session_doc.to_dict()
            
            # Count attendance
            present = db.collection('student_attendance') \
                .where('session_id', '==', session_doc.id) \
                .where('status', '==', 'present') \
                .stream()
            
            present_count = sum(1 for _ in present)
            
            # Get expected count
            expected = 0
            if session_data.get('section_id'):
                section_ref = db.collection('sections').document(session_data['section_id'])
                section_doc = section_ref.get()
                if section_doc.exists:
                    expected = section_doc.to_dict().get('total_students', 0)
            
            total_present += present_count
            total_expected += expected
            
            session_stats.append({
                'session_id': session_doc.id,
                'date': session_data.get('created_at'),
                'present': present_count,
                'expected': expected,
                'percentage': round((present_count / expected * 100) if expected > 0 else 0, 2)
            })
        
        overall_percentage = round((total_present / total_expected * 100) if total_expected > 0 else 0, 2)
        
        return {
            'subject_id': subject_id,
            'section_id': section_id,
            'period_days': days,
            'total_sessions': len(session_stats),
            'overall_attendance': overall_percentage,
            'sessions': session_stats
        }
    
    @staticmethod
    async def get_section_trends(section_id: str, days: int = 30):
        """
        Get attendance trends for a section
        
        Args:
            section_id: Section ID
            days: Number of days to look back
        
        Returns:
            Section attendance trends
        """
        db = AnalyticsService._get_db()
        
        cutoff_date = datetime.now() - timedelta(days=days)
        
        # Get section details
        section_ref = db.collection('sections').document(section_id)
        section_doc = section_ref.get()
        
        if not section_doc.exists:
            return {'error': 'Section not found'}
        
        section_data = section_doc.to_dict()
        total_students = section_data.get('total_students', 0)
        
        # Get all sessions for this section
        sessions = db.collection('sessions') \
            .where('section_id', '==', section_id) \
            .where('created_at', '>=', cutoff_date) \
            .stream()
        
        subject_wise = defaultdict(lambda: {'sessions': 0, 'total_present': 0})
        daily_attendance = []
        
        for session_doc in sessions:
            session_data = session_doc.to_dict()
            subject_id = session_data.get('subject_id')
            
            # Count attendance
            present = db.collection('student_attendance') \
                .where('session_id', '==', session_doc.id) \
                .where('status', '==', 'present') \
                .stream()
            
            present_count = sum(1 for _ in present)
            
            subject_wise[subject_id]['sessions'] += 1
            subject_wise[subject_id]['total_present'] += present_count
            
            daily_attendance.append({
                'date': session_data.get('created_at'),
                'subject_id': subject_id,
                'present': present_count,
                'percentage': round((present_count / total_students * 100) if total_students > 0 else 0, 2)
            })
        
        # Calculate subject-wise percentages
        subject_stats = []
        for subject_id, stats in subject_wise.items():
            expected_total = stats['sessions'] * total_students
            percentage = round((stats['total_present'] / expected_total * 100) if expected_total > 0 else 0, 2)
            
            subject_stats.append({
                'subject_id': subject_id,
                'sessions': stats['sessions'],
                'average_attendance': percentage
            })
        
        return {
            'section_id': section_id,
            'total_students': total_students,
            'period_days': days,
            'subject_wise': subject_stats,
            'daily_trends': sorted(daily_attendance, key=lambda x: x['date'], reverse=True)
        }
