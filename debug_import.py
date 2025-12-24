import sys
import os

# Setup path
sys.path.append(os.path.join(os.getcwd(), 'backend'))

# Setup Environment
os.environ['DATABASE_URL'] = 'sqlite:///:memory:'
os.environ['JWT_SECRET'] = 'test'

try:
    print("Attempting to import app.services.auth_service...")
    from app.services.auth_service import AuthService
    print("Import successful!")
except Exception as e:
    print(f"Import failed: {e}")
    import traceback
    traceback.print_exc()

try:
    print("Attempting to import backend.tests.unit.test_firebase_auth...")
    import backend.tests.unit.test_firebase_auth
    print("Test module import successful!")
except Exception as e:
    print(f"Test module import failed: {e}")
    import traceback
    traceback.print_exc()
