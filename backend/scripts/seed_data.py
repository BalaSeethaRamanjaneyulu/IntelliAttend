"""
Seed demo data for development and testing
"""
import sys
from pathlib import Path
from datetime import time

sys.path.append(str(Path(__file__).resolve().parents[1]))

from sqlalchemy.orm import Session
from app.db.database import SessionLocal
from app.models.student import Student
from app.models.faculty import Faculty
from app.models.class_model import Class
from app.models.room import Room
from app.core.security import get_password_hash


def seed_demo_data():
    """Insert demo data into database"""
    db: Session = SessionLocal()
    
    try:
        print("🌱 Seeding demo data...")
        
        # Create demo room
        room = Room(
            room_number="101",
            building="Main Block",
            floor=1,
            latitude=17.4435,  # Example coordinates
            longitude=78.3488,
            geofence_radius=30,
            wifi_ssid="Campus_WiFi",
            wifi_bssid="00:11:22:33:44:55",
            ble_beacons=["UUID-BEACON-1", "UUID-BEACON-2"]
        )
        db.add(room)
        db.flush()
        
        # Create demo class
        demo_class = Class(
            class_code="CSE101",
            name="Introduction to Computer Science",
            section="A",
            room_id=room.id,
            day_of_week=1,  # Monday
            start_time=time(9, 0),
            end_time=time(10, 30)
        )
        db.add(demo_class)
        db.flush()
        
        # Create demo faculty
        faculty = Faculty(
            faculty_id="FAC001",
            name="Dr. John Doe",
            email="john.doe@intelliattend.edu",
            password_hash=get_password_hash("demo123"[:72]),
            department="Computer Science"
        )
        db.add(faculty)
        
        # Create demo students
        students = [
            Student(
                student_id="STU001",
                name="Alice Johnson",
                email="alice@intelliattend.edu",
                password_hash=get_password_hash("demo123"[:72]),
                class_id=demo_class.id,
                device_id="DEVICE_ALICE_001"
            ),
            Student(
                student_id="STU002",
                name="Bob Smith",
                email="bob@intelliattend.edu",
                password_hash=get_password_hash("demo123"[:72]),
                class_id=demo_class.id,
                device_id="DEVICE_BOB_002"
            ),
            Student(
                student_id="STU003",
                name="Charlie Brown",
                email="charlie@intelliattend.edu",
                password_hash=get_password_hash("demo123"[:72]),
                class_id=demo_class.id,
                device_id="DEVICE_CHARLIE_003"
            )
        ]
        
        for student in students:
            db.add(student)
        
        db.commit()
        print("✅ Demo data seeded successfully!")
        print("\n📋 Demo Accounts:")
        print("Faculty:")
        print("  - Username: FAC001, Password: demo123")
        print("\nStudents:")
        print("  - Username: STU001, Password: demo123 (Alice)")
        print("  - Username: STU002, Password: demo123 (Bob)")
        print("  - Username: STU003, Password: demo123 (Charlie)")
        
    except Exception as e:
        db.rollback()
        print(f"❌ Error seeding data: {e}")
        sys.exit(1)
    finally:
        db.close()


if __name__ == "__main__":
    seed_demo_data()
