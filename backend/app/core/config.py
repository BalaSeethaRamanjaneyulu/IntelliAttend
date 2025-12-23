"""
Application Configuration using Pydantic Settings
Environment variables loaded from .env file
"""
from pydantic_settings import BaseSettings, SettingsConfigDict
from typing import List


class Settings(BaseSettings):
    """Application settings"""
    
    # Server
    ENVIRONMENT: str = "development"
    DEBUG: bool = True
    HOST: str = "0.0.0.0"
    PORT: int = 8000
    
    # Database
    DATABASE_URL: str
    
    # JWT
    JWT_SECRET: str
    JWT_ALGORITHM: str = "HS256"
    JWT_EXPIRES_IN: int = 86400  # 24 hours in seconds
    JWT_REFRESH_EXPIRES_IN: int = 604800  # 7 days
    
    # Session
    SESSION_DURATION_MINUTES: int = 2
    QR_REFRESH_INTERVAL_SECONDS: int = 5
    QR_TOKEN_EXPIRY_SECONDS: int = 7
    OTP_EXPIRY_MINUTES: int = 5
    OTP_LENGTH: int = 6
    
    # Verification Thresholds
    CONFIDENCE_THRESHOLD: float = 0.6
    BLE_RSSI_THRESHOLD: int = -70
    GPS_GEOFENCE_RADIUS_METERS: int = 30
    MIN_BLE_HITS: int = 2
    WARM_SCAN_DURATION_MINUTES: int = 3
    SCAN_INTERVAL_SECONDS: int = 30
    SCAN_DURATION_SECONDS: int = 12
    
    # Verification Weights
    WEIGHT_QR: float = 0.4
    WEIGHT_BLE: float = 0.3
    WEIGHT_WIFI: float = 0.2
    WEIGHT_GPS: float = 0.1
    
    # Security
    BCRYPT_ROUNDS: int = 12
    CORS_ORIGINS: List[str] = ["http://localhost:8080", "http://localhost:5173"]
    ALLOWED_HOSTS: List[str] = ["*"]
    
    # Logging
    LOG_LEVEL: str = "DEBUG"
    LOG_FILE: str = "logs/app.log"
    LOG_FORMAT: str = "%(asctime)s - %(name)s - %(levelname)s - %(message)s"
    
    model_config = SettingsConfigDict(
        env_file=".env",
        case_sensitive=True
    )


# Global settings instance
settings = Settings()
