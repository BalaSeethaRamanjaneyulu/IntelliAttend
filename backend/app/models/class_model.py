"""
Class model - represents academic classes/courses
"""
from sqlalchemy import Column, String, Integer, ForeignKey, Time
from sqlalchemy.orm import relationship

from app.db.base import Base


class Class(Base):
    __tablename__ = "classes"
    
    id = Column(Integer, primary_key=True, index=True)
    class_code = Column(String(50), unique=True, nullable=False, index=True)
    name = Column(String(100), nullable=False)
    section = Column(String(10), nullable=True)
    
    # Room assignment
    room_id = Column(Integer, ForeignKey("rooms.id"), nullable=False)
    
    # Schedule information
    day_of_week = Column(Integer, nullable=True)  # 0=Monday, 6=Sunday
    start_time = Column(Time, nullable=True)
    end_time = Column(Time, nullable=True)
    
    # Relationships
    room = relationship("Room", back_populates="classes")
    students = relationship("Student", back_populates="class_assigned")
    sessions = relationship("Session", back_populates="class_obj")
    
    def __repr__(self):
        return f"<Class {self.class_code}: {self.name}>"
