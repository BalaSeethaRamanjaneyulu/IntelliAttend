"""
Database initialization script
Creates all tables from SQLAlchemy models
"""
import sys
from pathlib import Path

# Add parent directory to path
sys.path.append(str(Path(__file__).resolve().parents[1]))

from app.db.database import engine
from app.models import Base
from app.core.config import settings


def init_db():
    """Initialize database - create all tables"""
    print("🗄️  Initializing database...")
    print(f"📊 Database: {settings.DATABASE_URL.split('@')[1]}")
    
    try:
        # Create all tables
        Base.metadata.create_all(bind=engine)
        print("✅ Database tables created successfully!")
        
        # List created tables
        print("\n📋 Tables created:")
        for table in Base.metadata.sorted_tables:
            print(f"  - {table.name}")
            
    except Exception as e:
        print(f"❌ Error creating database: {e}")
        sys.exit(1)


if __name__ == "__main__":
    init_db()
