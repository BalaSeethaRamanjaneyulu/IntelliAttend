"""
WebSocket routes for real-time updates - Firestore version
"""
from fastapi import APIRouter, WebSocket, WebSocketDisconnect
from app.services.websocket_manager import manager
from app.services.attendance_service import AttendanceService
import firebase_admin
from firebase_admin import firestore
from app.core import firebase
import asyncio
import json

router = APIRouter()

def _get_db():
    """Get Firestore client"""
    if not firebase_admin._apps:
        firebase.initialize_firebase()
    return firestore.client()


@router.websocket("/session/{session_id}")
async def websocket_session_endpoint(websocket: WebSocket, session_id: str):
    """
    WebSocket endpoint for real-time session updates
    
    Clients: SmartBoard portal, Faculty mobile app
    
    Messages sent to client:
    - attendance_update: Real-time attendance count
    - session_status: Session state changes
    - student_joined: When a student marks attendance
    
    Usage:
    const ws = new WebSocket('ws://localhost:8000/api/v1/websocket/session/SESSION_ID');
    ws.onmessage = (event) => {
        const data = JSON.parse(event.data);
        if (data.type === 'attendance_update') {
            updateCount(data.total_present);
        }
    };
    """
    db = _get_db()
    
    # Verify session exists
    session_ref = db.collection('sessions').document(session_id)
    session_doc = session_ref.get()
    
    if not session_doc.exists:
        await websocket.close(code=1008, reason="Session not found")
        return
    
    session_data = session_doc.to_dict()
    
    if session_data.get('status') != 'active':
        await websocket.close(code=1008, reason="Session not active")
        return
    
    # Connect client
    await manager.connect(websocket, session_id)
    
    # Send initial state
    attendance_count = await get_attendance_count(session_id)
    await manager.send_personal_message({
        "type": "connected",
        "session_id": session_id,
        "total_present": attendance_count,
        "message": "Connected to session"
    }, websocket)
    
    # Start QR Broadcast Loop
    qr_task = asyncio.create_task(qr_broadcast_loop(session_id))

    try:
        # Keep connection alive and listen for messages
        while True:
            data = await websocket.receive_text()
            
            try:
                message = json.loads(data)
                
                # Handle ping/pong
                if message.get('type') == 'ping':
                    await websocket.send_json({"type": "pong"})
                
                # Handle refresh request
                elif message.get('type') == 'refresh':
                    count = await get_attendance_count(session_id)
                    await websocket.send_json({
                        "type": "attendance_update",
                        "total_present": count
                    })
                
            except json.JSONDecodeError:
                await websocket.send_json({
                    "type": "error",
                    "message": "Invalid JSON"
                })
    
    except WebSocketDisconnect:
        manager.disconnect(websocket)
        qr_task.cancel()
    
    except Exception as e:
        print(f"WebSocket error: {e}")
        manager.disconnect(websocket)
        qr_task.cancel()


async def qr_broadcast_loop(session_id: str):
    """
    Background task to generate and broadcast QR tokens every 5 seconds
    """
    print(f"🔄 Starting QR Loop for {session_id} (Task Started)")
    try:
        from app.services.qr_service import QRService
        from app.core.config import settings
        from app.core.constants import QR_TOKEN_PREFIX
        import base64
        import json
        from datetime import datetime

        db = _get_db()
        
        sequence = 0
        
        while True:
            # Get latest session data
            session_ref = db.collection('sessions').document(session_id)
            session_doc = session_ref.get()
            
            if not session_doc.exists:
                print(f"❌ Session {session_id} not found in loop")
                break
                
            session_data = session_doc.to_dict()
            if session_data.get('status') != 'active':
                print(f"⏹️ Session {session_id} not active ({session_data.get('status')})")
                break
            
            # Use 'class_code' as subject or fallback
            subject_id = session_data.get('class_code', 'UNKNOWN')
            room_id = session_data.get('room_id', 'UNKNOWN-ROOM')
            
            sequence += 1
            timestamp = int(datetime.utcnow().timestamp())
            
            # ... payload construction ...
            
            payload_data = {
                "sid": session_id,
                "cid": session_data.get('class_id', 'UNKNOWN-CLASS'),
                "rid": room_id,
                "sub": subject_id,
                "seq": sequence,
                "ts": timestamp
            }
            
            # Serialize and encode payload
            json_str = json.dumps(payload_data, separators=(',', ':'))
            payload_b64 = base64.urlsafe_b64encode(json_str.encode('utf-8')).decode('utf-8').rstrip('=')
            
            # Generate HMAC signature
            signature = QRService._generate_signature(payload_b64)
            
            # Construct final token
            qr_token = f"{QR_TOKEN_PREFIX}_{payload_b64}_{signature}"
            
            print(f"📤 Broadcasting QR: {qr_token[:20]}...")
            
            # Broadcast to session clients
            await manager.broadcast_to_session(session_id, {
                "type": "qr_update",
                "qr_token": qr_token,
                "sequence_number": sequence,
                "timestamp": timestamp
            })
            
            await asyncio.sleep(5) # 5 Second Rotation
            
    except asyncio.CancelledError:
        print(f"QR Loop cancelled for session {session_id}")
    except Exception as e:
        import traceback
        traceback.print_exc()
        print(f"Error in QR loop for {session_id}: {e}")


async def get_attendance_count(session_id: str) -> int:
    """Get current attendance count for a session"""
    db = _get_db()
    
    attendance_records = db.collection('student_attendance') \
        .where('session_id', '==', session_id) \
        .where('status', '==', 'present') \
        .stream()
    
    return sum(1 for _ in attendance_records)


async def notify_attendance_marked(session_id: str, student_name: str = None):
    """
    Notify all connected clients that a student marked attendance
    
    Called from attendance_service.py after successful attendance marking
    """
    count = await get_attendance_count(session_id)
    await manager.send_attendance_update(session_id, count, student_name)
