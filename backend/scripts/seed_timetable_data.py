"""
Seed Firestore with Malla Reddy College timetable data
Run: python scripts/seed_timetable_data.py
"""
import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))

from app.core.firebase import initialize_firebase
import firebase_admin
from firebase_admin import firestore

if not firebase_admin._apps:
    initialize_firebase()

db = firestore.client()

print("🌱 Seeding IntelliAttend Database...")

# 1. Create Institution
print("\n📚 Creating Institution...")
db.collection('institutions').document('mrcet_001').set({
    'institution_id': 'mrcet_001',
    'name': 'Malla Reddy College of Engineering & Technology',
    'affiliation': 'Permanently Affiliated to JNTUH',
    'approvals': ['AICTE', 'NBA', 'NAAC-A-Grade', 'ISO 9001:2008'],
    'address': 'Maisammaguda, Dhulapally Post, Secunderabad–500100',
    'created_at': firestore.SERVER_TIMESTAMP
})

# 2. Create Department
print("🏢 Creating Department...")
db.collection('departments').document('dept_ci_001').set({
    'department_id': 'dept_ci_001',
    'institution_id': 'mrcet_001',
    'name': 'Department of Computational Intelligence',
    'school': 'School of Computer Science and Engineering-1',
    'created_at': firestore.SERVER_TIMESTAMP
})

# 3. Create Faculty
print("👨‍🏫 Creating Faculty...")
faculty_data = [
    {'id': 'faculty_kanniaah', 'name': 'Dr. Kanniaah', 'title': 'Dr.'},
    {'id': 'faculty_padmalatha', 'name': 'Dr. Padmalatha', 'title': 'Dr.'},
    {'id': 'faculty_santhosh', 'name': 'Mr. D. Santhosh Kumar', 'title': 'Mr.'},
    {'id': 'faculty_sateesh', 'name': 'Mr. N. Sateesh', 'title': 'Mr.'},
    {'id': 'faculty_arun', 'name': 'Dr. Arun Kumar', 'title': 'Dr.'},
    {'id': 'faculty_paromitha', 'name': 'Dr. Paromitha', 'title': 'Dr.'},
    {'id': 'faculty_radha', 'name': 'Dr. D. Radha', 'title': 'Dr.'},
    {'id': 'faculty_shilpa', 'name': 'Ms. V. Shilpa', 'title': 'Ms.'},
]

for faculty in faculty_data:
    db.collection('faculty').document(faculty['id']).set({
        'faculty_id': faculty['id'],
        'name': faculty['name'],
        'title': faculty['title'],
        'department_id': 'dept_ci_001',
        'email': f"{faculty['id']}@mrcet.ac.in",
        'role': 'faculty',
        'created_at': firestore.SERVER_TIMESTAMP
    })

# 4. Create Subjects
print("📖 Creating Subjects...")
subjects_data = [
    {'code': 'R22A6602', 'name': 'Machine Learning', 'short': 'ML', 'type': 'theory'},
    {'code': 'R22A6617', 'name': 'Design and Analysis of Computer Algorithms', 'short': 'DAA', 'type': 'theory'},
    {'code': 'R22A0512', 'name': 'Computer Networks', 'short': 'CN', 'type': 'theory'},
    {'code': 'R22A6702', 'name': 'Introduction to Data Science', 'short': 'IDS', 'type': 'theory'},
    {'code': 'R22A0351', 'name': 'Robotics and Automation', 'short': 'R&A', 'type': 'theory'},
    {'code': 'R22A0084', 'name': 'Professional Skill Development', 'short': 'PSD', 'type': 'theory'},
    {'code': 'R22A6681', 'name': 'Machine Learning Lab', 'short': 'ML LAB', 'type': 'lab'},
    {'code': 'R22A0596', 'name': 'Computer Networks Lab', 'short': 'CN LAB', 'type': 'lab'},
    {'code': 'R22A6692', 'name': 'Application Development – 1', 'short': 'AD-1', 'type': 'lab'},
    {'code': 'R22A0511', 'name': 'Compiler Design', 'short': 'CD', 'type': 'theory'},
    {'code': 'R22A6601', 'name': 'Artificial Intelligence', 'short': 'AI', 'type': 'theory'},
    {'code': 'R22A0513', 'name': 'Full Stack Development', 'short': 'FSD', 'type': 'theory'},
    {'code': 'R22A0514', 'name': 'Distributed Systems', 'short': 'DS', 'type': 'theory'},
]

for subject in subjects_data:
    db.collection('subjects').document(subject['code']).set({
        'subject_id': subject['code'],
        'code': subject['code'],
        'name': subject['name'],
        'short_name': subject['short'],
        'department_id': 'dept_ci_001',
        'type': subject['type'],
        'created_at': firestore.SERVER_TIMESTAMP
    })

# 5. Create Section A (Computational Intelligence 2025-26)
print("🎓 Creating Section A...")
db.collection('sections').document('ci_2025_a').set({
    'section_id': 'ci_2025_a',
    'department_id': 'dept_ci_001',
    'section_name': 'A',
    'academic_year': '2025-26',
    'semester': 'I-Sem',
    'room_number': '4208',
    'class_incharge_id': 'faculty_kanniaah',
    'total_students': 60,
    'created_at': firestore.SERVER_TIMESTAMP
})

# 6. Create Timetable Slots for Section A
print("📅 Creating Timetable Slots for Section A...")
timetable_a = [
    # Monday
    {'day': 'Monday', 'slot': 1, 'start': '09:20', 'end': '10:30', 'subject': 'R22A0512', 'faculty': 'faculty_santhosh'},
    {'day': 'Monday', 'slot': 2, 'start': '10:30', 'end': '11:40', 'subject': 'R22A0351', 'faculty': 'faculty_arun'},
    {'day': 'Monday', 'slot': 3, 'start': '11:50', 'end': '13:00', 'subject': 'R22A6602', 'faculty': 'faculty_kanniaah'},
    {'day': 'Monday', 'slot': 4, 'start': '13:50', 'end': '14:50', 'subject': 'R22A6681', 'faculty': 'faculty_kanniaah'},
    # Tuesday
    {'day': 'Tuesday', 'slot': 1, 'start': '09:20', 'end': '10:30', 'subject': 'R22A6617', 'faculty': 'faculty_padmalatha'},
    {'day': 'Tuesday', 'slot': 2, 'start': '10:30', 'end': '11:40', 'subject': 'R22A0512', 'faculty': 'faculty_santhosh'},
    {'day': 'Tuesday', 'slot': 3, 'start': '11:50', 'end': '13:00', 'subject': 'R22A6702', 'faculty': 'faculty_sateesh'},
    {'day': 'Tuesday', 'slot': 4, 'start': '13:50', 'end': '14:50', 'subject': 'R22A0084', 'faculty': 'faculty_paromitha'},
    {'day': 'Tuesday', 'slot': 5, 'start': '14:50', 'end': '15:50', 'subject': 'R22A6602', 'faculty': 'faculty_kanniaah'},
    # Add more days as needed...
]

for slot_data in timetable_a:
    db.collection('timetable_slots').add({
        'section_id': 'ci_2025_a',
        'day': slot_data['day'],
        'slot_number': slot_data['slot'],
        'start_time': slot_data['start'],
        'end_time': slot_data['end'],
        'subject_id': slot_data['subject'],
        'faculty_id': slot_data['faculty'],
        'room_number': '4208',
        'is_break': False,
        'is_lab': 'LAB' in slot_data['subject'],
        'created_at': firestore.SERVER_TIMESTAMP
    })

# 7. Create Section-Subject Mappings
print("🔗 Creating Section-Subject Mappings...")
mappings = [
    {'section': 'ci_2025_a', 'subject': 'R22A6602', 'faculty': 'faculty_kanniaah'},
    {'section': 'ci_2025_a', 'subject': 'R22A6617', 'faculty': 'faculty_padmalatha'},
    {'section': 'ci_2025_a', 'subject': 'R22A0512', 'faculty': 'faculty_santhosh'},
    {'section': 'ci_2025_a', 'subject': 'R22A6702', 'faculty': 'faculty_sateesh'},
    {'section': 'ci_2025_a', 'subject': 'R22A0351', 'faculty': 'faculty_arun'},
]

for mapping in mappings:
    db.collection('section_subjects').add({
        'section_id': mapping['section'],
        'subject_id': mapping['subject'],
        'faculty_id': mapping['faculty'],
        'academic_year': '2025-26',
        'created_at': firestore.SERVER_TIMESTAMP
    })

print("\n✅ Database seeded successfully!")
print("📊 Collections created:")
print("   - institutions (1)")
print("   - departments (1)")
print("   - faculty (8)")
print("   - subjects (13)")
print("   - sections (1)")
print("   - timetable_slots (9)")
print("   - section_subjects (5)")
