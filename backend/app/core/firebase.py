import firebase_admin
from firebase_admin import credentials, auth
import os
from app.core.config import settings

def initialize_firebase():
    """Initialize Firebase Admin SDK"""
    try:
        # Check if already initialized to avoid error on reload
        if not firebase_admin._apps:
            cred = None
            
            # Check for GOOGLE_APPLICATION_CREDENTIALS in settings or env
            google_creds_path = settings.GOOGLE_APPLICATION_CREDENTIALS or os.getenv("GOOGLE_APPLICATION_CREDENTIALS") or "serviceAccountKey.json"
            
            if os.path.exists(google_creds_path):
                cred = credentials.Certificate(google_creds_path)
            else:
                print(f"Warning: {google_creds_path} not found. Attempting to use default credentials.")
                cred = credentials.ApplicationDefault()

            firebase_admin.initialize_app(cred)
            print("Firebase Admin SDK initialized successfully")
            
    except Exception as e:
        print(f"Failed to initialize Firebase: {str(e)}")

# Initialize on module import or call explicitly in startup event
initialize_firebase()
