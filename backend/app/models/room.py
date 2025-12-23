"""
Room model - represents physical classrooms with geolocation and network info
"""
from sqlalchemy import Column, String, Integer, Float, JSON
from sqlalchemy.orm import relationship

from app.db.base import Base


class Room(Base):
    __tablename__ = "rooms"
    
    id = Column(Integer, primary_key=True, index=True)
    room_number = Column(String(50), unique=True, nullable=False, index=True)
    building = Column(String(100), nullable=True)
    floor = Column(Integer, nullable=True)
    
    # GPS Coordinates for geofencing
    latitude = Column(Float, nullable=False)
    longitude = Column(Float, nullable=False)
    geofence_radius = Column(Integer, default=30)  # meters
    
    # Wi-Fi Information
    wifi_ssid = Column(String(100), nullable=True)
    wifi_bssid = Column(String(17), nullable=True)  # MAC address format
    
    # Bluetooth Beacons (stored as JSON array of UUIDs)
    # Example: ["UUID-1", "UUID-2", "UUID-3"]
    ble_beacons = Column(JSON, nullable=True)
    
    # Relationships
    classes = relationship("Class", back_populates="room")
    
    def __repr__(self):
        return f"<Room {self.room_number}: {self.building}>"
