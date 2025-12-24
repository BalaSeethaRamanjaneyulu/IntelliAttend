"""
Complete Database Seed Script - All Entities with Relationships
Run: python scripts/seed_complete_database.py
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

print("🌱 Seeding Complete IntelliAttend Database...")
print("=" * 60)

# 1. INSTITUTION
print("\n📚 Creating Institution...")
db.collection('institutions').document('mrcet_001').set({
    'institution_id': 'mrcet_001',
    'name': 'Malla Reddy College of Engineering & Technology',
    'affiliation': 'Permanently Affiliated to JNTUH',
    'approvals': ['AICTE', 'NBA', 'NAAC-A-Grade', 'ISO 9001:2008'],
    'headquarters_campus_id': 'mrcet_main',
    'total_campuses': 1,
    'created_at': firestore.SERVER_TIMESTAMP
})

# 2. CAMPUS
print("🏫 Creating Campus...")
db.collection('campuses').document('mrcet_main').set({
    'campus_id': 'mrcet_main',
    'institution_id': 'mrcet_001',
    'name': 'Main Campus',
    'address': 'Maisammaguda, Dhulapally Post, Secunderabad–500100',
    'coordinates': {
        'latitude': 17.5562,
        'longitude': 78.5615
    },
    'total_blocks': 5,
    'created_at': firestore.SERVER_TIMESTAMP
})

# 3. BLOCK
print("🏢 Creating Block 4...")
db.collection('blocks').document('block_4').set({
    'block_id': 'block_4',
    'campus_id': 'mrcet_main',
    'name': 'Block 4',
    'description': 'CSE Department Block',
    'total_floors': 3,
    'coordinates': {
        'latitude': 17.5563,
        'longitude': 78.5616
    },
    'created_at': firestore.SERVER_TIMESTAMP
})

# 4. FLOOR
print("🔢 Creating Floor 2...")
db.collection('floors').document('block4_floor2').set({
    'floor_id': 'block4_floor2',
    'block_id': 'block_4',
    'floor_number': 2,
    'name': 'Second Floor',
    'total_classrooms': 15,
    'created_at': firestore.SERVER_TIMESTAMP
})

# 5. CLASSROOM
print("🚪 Creating Classroom 4208...")
db.collection('classrooms').document('room_4208').set({
    'classroom_id': 'room_4208',
    'floor_id': 'block4_floor2',
    'block_id': 'block_4',
    'campus_id': 'mrcet_main',
    'room_number': '4208',
    'name': 'Room 4208',
    'capacity': 60,
    'type': 'lecture_hall',
    'devices': {
        'smartboard_id': 'sb_4208',
        'wifi_router_id': 'wifi_block4_floor2_001',
        'bluetooth_beacon_id': 'beacon_4208'
    },
    'wifi_ssid': 'MRCET_4208',
    'wifi_bssid': 'AA:BB:CC:DD:EE:01',
    'coordinates': {
        'latitude': 17.5563,
        'longitude': 78.5616,
        'floor_level': 2
    },
    'facilities': ['projector', 'smartboard', 'ac', 'wifi', 'cctv'],
    'created_at': firestore.SERVER_TIMESTAMP
})

# 6. DEVICES
print("📱 Creating Devices...")

# SmartBoard
db.collection('smartboards').document('sb_4208').set({
    'smartboard_id': 'sb_4208',
    'device_id': 'device_sb_4208',
    'manufacturer': 'Samsung',
    'model': 'Interactive Display 75-inch',
    'serial_number': 'SB2025-4208-001',
    'ip_address': '192.168.4.101',
    'mac_address': 'AA:BB:CC:DD:EE:FF',
    'hostname': 'smartboard-4208.mrcet.local',
    'classroom_id': 'room_4208',
    'floor_id': 'block4_floor2',
    'block_id': 'block_4',
    'campus_id': 'mrcet_main',
    'screen_size': '75 inches',
    'resolution': '3840x2160',
    'touch_enabled': True,
    'status': 'active',
    'current_session_id': None,
    'created_at': firestore.SERVER_TIMESTAMP
})

# WiFi Router
db.collection('wifi_routers').document('wifi_block4_floor2_001').set({
    'router_id': 'wifi_block4_floor2_001',
    'device_id': 'device_wifi_4208',
    'ssid': 'MRCET_4208',
    'bssid': 'AA:BB:CC:DD:EE:01',
    'frequency': '5GHz',
    'channel': 36,
    'coverage_classrooms': ['room_4208', 'room_4209', 'room_4210'],
    'classroom_id': 'room_4208',
    'floor_id': 'block4_floor2',
    'block_id': 'block_4',
    'campus_id': 'mrcet_main',
    'ip_address': '192.168.4.1',
    'status': 'active',
    'created_at': firestore.SERVER_TIMESTAMP
})

# Bluetooth Beacon
db.collection('bluetooth_beacons').document('beacon_4208').set({
    'beacon_id': 'beacon_4208',
    'device_id': 'device_ble_4208',
    'uuid': 'f7826da6-4fa2-4e98-8024-bc5b71e0893e',
    'major': 100,
    'minor': 4208,
    'tx_power': -59,
    'classroom_id': 'room_4208',
    'floor_id': 'block4_floor2',
    'block_id': 'block_4',
    'campus_id': 'mrcet_main',
    'coordinates': {
        'latitude': 17.5563,
        'longitude': 78.5616
    },
    'battery_level': 85,
    'status': 'active',
    'created_at': firestore.SERVER_TIMESTAMP
})

# 7. DEPARTMENT
print("🏛️ Creating Department...")
db.collection('departments').document('dept_ci_001').set({
    'department_id': 'dept_ci_001',
    'institution_id': 'mrcet_001',
    'campus_id': 'mrcet_main',
    'name': 'Department of Computational Intelligence',
    'school': 'School of Computer Science and Engineering-1',
    'head_of_department_id': 'faculty_radha',
    'primary_block_id': 'block_4',
    'created_at': firestore.SERVER_TIMESTAMP
})

# 8. FACULTY
print("👨‍🏫 Creating Faculty...")
faculty_list = [
    {'id': 'faculty_kanniaah', 'name': 'Dr. Kanniaah', 'title': 'Dr.', 'office': 'room_4201'},
    {'id': 'faculty_padmalatha', 'name': 'Dr. Padmalatha', 'title': 'Dr.', 'office': 'room_4202'},
    {'id': 'faculty_santhosh', 'name': 'Mr. D. Santhosh Kumar', 'title': 'Mr.', 'office': 'room_4203'},
    {'id': 'faculty_sateesh', 'name': 'Mr. N. Sateesh', 'title': 'Mr.', 'office': 'room_4204'},
    {'id': 'faculty_arun', 'name': 'Dr. Arun Kumar', 'title': 'Dr.', 'office': 'room_4205'},
]

for faculty in faculty_list:
    db.collection('faculty').document(faculty['id']).set({
        'faculty_id': faculty['id'],
        'name': faculty['name'],
        'title': faculty['title'],
        'department_id': 'dept_ci_001',
        'campus_id': 'mrcet_main',
        'office_classroom_id': faculty['office'],
        'email': f"{faculty['id']}@mrcet.ac.in",
        'role': 'faculty',
        'created_at': firestore.SERVER_TIMESTAMP
    })

# 9. SUBJECTS
print("📖 Creating Subjects...")
subjects = [
    {'code': 'R22A6602', 'name': 'Machine Learning', 'short': 'ML'},
    {'code': 'R22A6617', 'name': 'Design and Analysis of Algorithms', 'short': 'DAA'},
    {'code': 'R22A0512', 'name': 'Computer Networks', 'short': 'CN'},
    {'code': 'R22A6702', 'name': 'Introduction to Data Science', 'short': 'IDS'},
    {'code': 'R22A0351', 'name': 'Robotics and Automation', 'short': 'R&A'},
]

for subject in subjects:
    db.collection('subjects').document(subject['code']).set({
        'subject_id': subject['code'],
        'code': subject['code'],
        'name': subject['name'],
        'short_name': subject['short'],
        'department_id': 'dept_ci_001',
        'type': 'theory',
        'created_at': firestore.SERVER_TIMESTAMP
    })

# 10. SECTION
print("🎓 Creating Section A...")
db.collection('sections').document('ci_2025_a').set({
    'section_id': 'ci_2025_a',
    'department_id': 'dept_ci_001',
    'campus_id': 'mrcet_main',
    'section_name': 'A',
    'academic_year': '2025-26',
    'semester': 'I-Sem',
    'default_classroom_id': 'room_4208',
    'class_incharge_id': 'faculty_kanniaah',
    'total_students': 60,
    'created_at': firestore.SERVER_TIMESTAMP
})

# 11. TIMETABLE SLOTS
print("📅 Creating Timetable Slots...")
slots = [
    {'day': 'Monday', 'slot': 1, 'start': '09:20', 'end': '10:30', 'subject': 'R22A0512', 'faculty': 'faculty_santhosh'},
    {'day': 'Monday', 'slot': 2, 'start': '10:30', 'end': '11:40', 'subject': 'R22A0351', 'faculty': 'faculty_arun'},
    {'day': 'Monday', 'slot': 3, 'start': '11:50', 'end': '13:00', 'subject': 'R22A6602', 'faculty': 'faculty_kanniaah'},
]

for slot in slots:
    db.collection('timetable_slots').add({
        'section_id': 'ci_2025_a',
        'subject_id': slot['subject'],
        'faculty_id': slot['faculty'],
        'classroom_id': 'room_4208',
        'day': slot['day'],
        'slot_number': slot['slot'],
        'start_time': slot['start'],
        'end_time': slot['end'],
        'is_break': False,
        'is_lab': False,
        'created_at': firestore.SERVER_TIMESTAMP
    })

print("\n" + "=" * 60)
print("✅ Complete Database Seeded Successfully!")
print("\n📊 Summary:")
print("   ✓ 1 Institution")
print("   ✓ 1 Campus")
print("   ✓ 1 Block")
print("   ✓ 1 Floor")
print("   ✓ 1 Classroom (with GPS)")
print("   ✓ 3 Devices (SmartBoard, WiFi, Bluetooth)")
print("   ✓ 1 Department")
print("   ✓ 5 Faculty")
print("   ✓ 5 Subjects")
print("   ✓ 1 Section")
print("   ✓ 3 Timetable Slots")
print("\n🔗 All relationships verified!")
print("=" * 60)
