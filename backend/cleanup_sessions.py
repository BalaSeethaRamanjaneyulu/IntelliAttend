#!/usr/bin/env python3
"""
Cleanup script to end all active sessions in Firestore
Use this to reset the system before testing
"""
import asyncio
from app.core.firebase import initialize_firebase
from google.cloud import firestore

async def cleanup_active_sessions():
    """End all active sessions in Firestore"""
    initialize_firebase()
    db = firestore.Client()
    
    print("🧹 Cleaning up active sessions...")
    
    # Get all active sessions
    active_sessions = db.collection('ActiveSessions').where('status', '==', 'active').stream()
    
    count = 0
    for session_doc in active_sessions:
        session_id = session_doc.id
        print(f"   Ending session: {session_id}")
        
        # Update status to expired
        db.collection('ActiveSessions').document(session_id).update({
            'status': 'expired'
        })
        count += 1
    
    print(f"✅ Cleaned up {count} active session(s)")
    print("🎯 System ready for fresh testing!")

if __name__ == "__main__":
    asyncio.run(cleanup_active_sessions())
