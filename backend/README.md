# IntelliAttend Backend - Python/FastAPI

## Prerequisites
- Python 3.10+
- MySQL 8.0+
- pip

## Setup

### 1. Create Virtual Environment
```bash
python3 -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
```

### 2. Install Dependencies
```bash
pip install -r requirements.txt
```

### 3. Configure Environment
```bash
cp ../.env.example .env
# Edit .env with your MySQL credentials
```

### 4. Initialize Database
```bash
# Create tables
python scripts/init_db.py

# OR use Alembic migrations (recommended)
alembic upgrade head

# Seed demo data
python scripts/seed_data.py
```

### 5. Run Development Server
```bash
uvicorn main:app --reload
```

Server will start at http://localhost:8000

## API Documentation
- Swagger UI: http://localhost:8000/docs
- ReDoc: http://localhost:8000/redoc

## Demo Accounts
- **Faculty**: FAC001 / demo123
- **Students**: STU001, STU002, STU003 / demo123

## Alembic Migrations
```bash
# Create new migration
alembic revision --autogenerate -m "description"

# Apply migrations
alembic upgrade head

# Rollback
alembic downgrade -1
```

## Testing
```bash
pytest tests/ -v
```

## Docker Development
```bash
cd ..
docker-compose up -d backend
```
