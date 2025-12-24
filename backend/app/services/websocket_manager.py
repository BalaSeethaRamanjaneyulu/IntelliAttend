"""
WebSocket Manager - Handle real-time connections
"""
from fastapi import WebSocket
from typing import Dict, Set
import json
import asyncio

class ConnectionManager:
    """Manage WebSocket connections for real-time updates"""
    
    def __init__(self):
        # session_id -> set of WebSocket connections
        self.active_connections: Dict[str, Set[WebSocket]] = {}
        # websocket -> session_id mapping
        self.connection_sessions: Dict[WebSocket, str] = {}
    
    async def connect(self, websocket: WebSocket, session_id: str):
        """Accept new WebSocket connection"""
        await websocket.accept()
        
        if session_id not in self.active_connections:
            self.active_connections[session_id] = set()
        
        self.active_connections[session_id].add(websocket)
        self.connection_sessions[websocket] = session_id
        
        print(f"✅ WebSocket connected for session: {session_id}")
    
    def disconnect(self, websocket: WebSocket):
        """Remove WebSocket connection"""
        session_id = self.connection_sessions.get(websocket)
        
        if session_id and session_id in self.active_connections:
            self.active_connections[session_id].discard(websocket)
            
            # Clean up empty session
            if not self.active_connections[session_id]:
                del self.active_connections[session_id]
        
        if websocket in self.connection_sessions:
            del self.connection_sessions[websocket]
        
        print(f"❌ WebSocket disconnected for session: {session_id}")
    
    async def send_personal_message(self, message: dict, websocket: WebSocket):
        """Send message to specific connection"""
        try:
            await websocket.send_json(message)
        except Exception as e:
            print(f"Error sending message: {e}")
            self.disconnect(websocket)
    
    async def broadcast_to_session(self, session_id: str, message: dict):
        """Broadcast message to all connections for a session"""
        if session_id not in self.active_connections:
            return
        
        disconnected = set()
        
        for connection in self.active_connections[session_id]:
            try:
                await connection.send_json(message)
            except Exception as e:
                print(f"Error broadcasting: {e}")
                disconnected.add(connection)
        
        # Clean up disconnected connections
        for connection in disconnected:
            self.disconnect(connection)
    
    async def send_attendance_update(self, session_id: str, student_count: int, student_name: str = None):
        """Send attendance count update to all connected clients"""
        message = {
            "type": "attendance_update",
            "session_id": session_id,
            "total_present": student_count,
            "timestamp": asyncio.get_event_loop().time()
        }
        
        if student_name:
            message["latest_student"] = student_name
        
        await self.broadcast_to_session(session_id, message)
    
    async def send_session_status(self, session_id: str, status: str, data: dict = None):
        """Send session status update"""
        message = {
            "type": "session_status",
            "session_id": session_id,
            "status": status,
            "data": data or {},
            "timestamp": asyncio.get_event_loop().time()
        }
        
        await self.broadcast_to_session(session_id, message)
    
    def get_connection_count(self, session_id: str) -> int:
        """Get number of active connections for a session"""
        return len(self.active_connections.get(session_id, set()))


# Global connection manager instance
manager = ConnectionManager()
