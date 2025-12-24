"""
WebSocket routes for real-time updates - Phase 3 implementation
"""
from fastapi import APIRouter, WebSocket, WebSocketDisconnect, Depends
from sqlalchemy.orm import Session
from typing import Dict, Set
import asyncio
import json

from app.db.database import get_db
from app.services.session_service import SessionService
from app.services.qr_service import QRService
from app.core.config import settings

router = APIRouter()

# Active WebSocket connections per session
active_connections: Dict[str, Set[WebSocket]] = {}


class ConnectionManager:
    """Manage WebSocket connections for sessions"""
    
    def __init__(self):
        self.active_connections: Dict[str, Set[WebSocket]] = {}
    
    async def connect(self, websocket: WebSocket, session_id: str):
        """Accept and store WebSocket connection"""
        await websocket.accept()
        
        if session_id not in self.active_connections:
            self.active_connections[session_id] = set()
        
        self.active_connections[session_id].add(websocket)
        print(f"[WebSocket] Client connected to session: {session_id}")
    
    def disconnect(self, websocket: WebSocket, session_id: str):
        """Remove WebSocket connection"""
        if session_id in self.active_connections:
            self.active_connections[session_id].discard(websocket)
            
            if not self.active_connections[session_id]:
                del self.active_connections[session_id]
        
        print(f"[WebSocket] Client disconnected from session: {session_id}")
    
    async def broadcast_to_session(self, session_id: str, message: dict):
        """Broadcast message to all clients in a session"""
        if session_id not in self.active_connections:
            return
        
        # Remove disconnected clients
        dead_connections = set()
        
        for connection in self.active_connections[session_id]:
            try:
                await connection.send_json(message)
            except Exception as e:
                print(f"[WebSocket] Error sending to client: {e}")
                dead_connections.add(connection)
        
        # Clean up dead connections
        for conn in dead_connections:
            self.active_connections[session_id].discard(conn)


manager = ConnectionManager()


@router.websocket("/session/{session_id}")
async def websocket_session(
    websocket: WebSocket,
    session_id: str,
    db: Session = Depends(get_db)
):
    """
    WebSocket endpoint for real-time session updates
    
    Clients: SmartBoard portal, Faculty app
    
    Messages sent to client:
    - qr_update: New QR token every 5 seconds
    - attendance_update: Real-time attendance stats
    - session_status: Session state changes
    
    Usage:
    const ws = new WebSocket('ws://localhost:8000/ws/session/SESS_123');
    ws.onmessage = (event) => {
        const data = JSON.parse(event.data);
        // Handle qr_update, attendance_update, session_status
    };
    """
    # Verify session exists and is active
    try:
        session = SessionService.get_active_session(db, session_id)
    except Exception as e:
        await websocket.close(code=1008, reason=f"Invalid session: {str(e)}")
        return
    
    # Connect client
    await manager.connect(websocket, session_id)
    
    # Start QR token refresh task
    qr_task = asyncio.create_task(
        qr_refresh_loop(websocket, session_id, session.id, db)
    )
    
    try:
        # Keep connection alive and listen for messages
        while True:
            data = await websocket.receive_text()
            
            # Handle client messages (future: control commands)
            try:
                message = json.loads(data)
                print(f"[WebSocket] Received from client: {message}")
                
                # Echo back for now
                await websocket.send_json({
                    "type": "ack",
                    "message": "Message received"
                })
                
            except json.JSONDecodeError:
                await websocket.send_json({
                    "type": "error",
                    "message": "Invalid JSON"
                })
    
    except WebSocketDisconnect:
        manager.disconnect(websocket, session_id)
        qr_task.cancel()
        print(f"[WebSocket] Client disconnected: {session_id}")
    
    except Exception as e:
        print(f"[WebSocket] Error: {e}")
        manager.disconnect(websocket, session_id)
        qr_task.cancel()


async def qr_refresh_loop(
    websocket: WebSocket,
    session_id: str,
    session_db_id: int,
    db: Session
):
    """
    Background task to refresh QR tokens every 5 seconds
    
    Args:
        websocket: Client WebSocket connection
        session_id: Session ID string
        session_db_id: Session database ID
        db: Database session
    """
    try:
        while True:
            # Get fresh session from DB
            session = db.query(SessionModel).filter(
                SessionModel.id == session_db_id
            ).first()
            
            if not session or session.status != SessionStatusEnum.ACTIVE:
                # Session ended
                await websocket.send_json({
                    "type": "session_status",
                    "status": "completed" if session else "not_found"
                })
                break
            
            # Generate new QR token
            qr_token, sequence = QRService.generate_qr_token(db, session)
            
            # ============================================
            # DETAILED TOKEN LOGGING FOR DEBUGGING
            # ============================================
            print(f"\n{'='*60}")
            print(f"[QR TOKEN GENERATED] Cycle #{sequence}")
            print(f"{'='*60}")
            print(f"Session ID: {session_id}")
            print(f"Encrypted Token:")
            print(f"  {qr_token}")
            print(f"Token Length: {len(qr_token)} chars")
            print(f"Expires in: {settings.QR_TOKEN_EXPIRY_SECONDS} seconds")
            print(f"{'='*60}\n")
            
            # Broadcast to all clients
            await manager.broadcast_to_session(session_id, {
                "type": "qr_update",
                "qr_token": qr_token,
                "sequence_number": sequence,
                "expires_in": settings.QR_TOKEN_EXPIRY_SECONDS
            })
            
            print(f"[WebSocket] Broadcasted QR update #{sequence} to session {session_id}")
            
            # Wait 5 seconds before next refresh
            await asyncio.sleep(settings.QR_REFRESH_INTERVAL_SECONDS)
    
    except asyncio.CancelledError:
        print(f"[WebSocket] QR refresh task cancelled for session {session_id}")
    except Exception as e:
        print(f"[WebSocket] Error in QR refresh loop: {e}")


async def broadcast_attendance_update(session_id: str, stats: dict, students: list = None):
    """
    Broadcast attendance update to all clients in a session
    
    Called when:
    - Student submits attendance
    - Attendance status changes (manual override)
    
    Args:
        session_id: Session ID to broadcast to
        stats: Attendance statistics dict
        students: Optional list of student statuses
    """
    message = {
        "type": "attendance_update",
        "stats": stats
    }
    
    if students:
        message["students"] = students
        
    await manager.broadcast_to_session(session_id, message)


# Import after definition to avoid circular import
from app.models.session import Session as SessionModel, SessionStatusEnum
