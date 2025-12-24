"""
Token Rotation Scheduler
Background service that rotates QR tokens every 5 seconds for all active sessions
"""
from apscheduler.schedulers.asyncio import AsyncIOScheduler
from apscheduler.triggers.interval import IntervalTrigger
from app.services.active_sessions_service import ActiveSessionsService
from app.services.websocket_manager import manager
from datetime import datetime
import asyncio


class TokenRotationScheduler:
    """
    Background scheduler for automatic QR token rotation
    
    Runs every 5 seconds:
    1. Fetches all active sessions from Firestore
    2. Generates new token for each session
    3. Updates ActiveSessions collection
    4. Broadcasts token to WebSocket clients (SmartBoard)
    """
    
    def __init__(self):
        self.scheduler = AsyncIOScheduler()
        self.is_running = False
        
    def start(self):
        """Start the token rotation scheduler"""
        if self.is_running:
            print("⚠️ Token rotation scheduler already running")
            return
            
        # STABILIZATION: Run a quick cleanup of orphaned sessions on startup
        try:
            from app.core.firebase import initialize_firebase
            from firebase_admin import firestore
            initialize_firebase()
            db = firestore.client()
            active_sessions = db.collection('ActiveSessions').where('status', '==', 'active').get()
            count = 0
            for doc in active_sessions:
                doc.reference.update({'status': 'expired', 'endedAt': firestore.SERVER_TIMESTAMP})
                count += 1
            if count > 0:
                print(f"🧹 Startup Cleanup: Expired {count} orphaned session(s)")
            else:
                print("✨ Startup Check: No orphaned sessions found")
        except Exception as e:
            print(f"⚠️ Startup Cleanup Warning: {e}")
        
        # Schedule rotation every 5 seconds
        self.scheduler.add_job(
            self._rotate_all_tokens,
            trigger=IntervalTrigger(seconds=5),
            id='token_rotation',
            name='QR Token Rotation',
            replace_existing=True
        )
        
        self.scheduler.start()
        self.is_running = True
        print("✅ Token rotation scheduler started (5-second interval)")
    
    def stop(self):
        """Stop the token rotation scheduler"""
        if not self.is_running:
            return
        
        self.scheduler.shutdown()
        self.is_running = False
        print("⏹️ Token rotation scheduler stopped")
    
    async def _rotate_all_tokens(self):
        """
        Rotate tokens for all active sessions
        Called every 5 seconds by scheduler
        """
        try:
            # Get all active session IDs
            active_session_ids = await ActiveSessionsService.get_all_active_sessions()
            
            if not active_session_ids:
                # HEARTBEAT: Show that scheduler is alive but idle
                print("💤 Scheduler Heartbeat: Idle (No active sessions)")
                return
            
            # Filter unique IDs to prevent duplicate rotations in edge cases
            active_session_ids = list(set(active_session_ids))
            
            print(f"📡 Scheduler ACTIVE: Rotating {len(active_session_ids)} session(s)...")
            
            # Rotate token for each session
            for session_id in active_session_ids:
                try:
                    # Rotate token in Firestore
                    new_token_data = await ActiveSessionsService.rotate_token(session_id)
                    
                    if new_token_data:
                        # Broadcast to WebSocket clients (SmartBoard portal)
                        await self._broadcast_token_update(session_id, new_token_data)
                        
                except Exception as e:
                    print(f"❌ Critical error rotating token for session {session_id}: {e}")
                    continue
            
        except Exception as e:
            print(f"❌ Error in token rotation scheduler: {e}")
    
    async def _broadcast_token_update(self, session_id: str, token_data: dict):
        """
        Broadcast new token to WebSocket clients (SmartBoard)
        
        Students will get tokens via Firestore listeners, not WebSocket
        This is only for SmartBoard QR display
        
        Args:
            session_id: Session identifier
            token_data: New token data from TokenGenerator
        """
        try:
            message = {
                "type": "qr_update",
                "qr_token": token_data['token'],
                "sequence_number": token_data['sequence'],
                "timestamp": token_data['timestamp'],
                "expiry": token_data['expiry']
            }
            
            # Broadcast to all WebSocket connections for this session
            await manager.broadcast_to_session(session_id, message)
            
        except Exception as e:
            print(f"⚠️ Error broadcasting token to WebSocket: {e}")


# Global scheduler instance
_scheduler_instance = None


def get_scheduler() -> TokenRotationScheduler:
    """Get or create global scheduler instance"""
    global _scheduler_instance
    if _scheduler_instance is None:
        _scheduler_instance = TokenRotationScheduler()
    return _scheduler_instance


def start_token_rotation():
    """Start the global token rotation scheduler"""
    scheduler = get_scheduler()
    scheduler.start()


def stop_token_rotation():
    """Stop the global token rotation scheduler"""
    scheduler = get_scheduler()
    scheduler.stop()
