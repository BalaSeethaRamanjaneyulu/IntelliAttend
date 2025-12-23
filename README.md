```
## 🎯 Project Overview

# IntelliAttend - Smart Attendance Management System

Smart, automated, and tamper-proof attendance management using multi-factor verification with Dynamic QR Codes, Bluetooth proximity, Wi-Fi BSSID, and GPS geofencing.

**🎯 Current Status: ~90% Complete - Ready for Testing!**
- ✅ Backend API (Phases 1-3) - 100%
- ✅ SmartBoard Portal (Foundation) - 60%
- ✅ Student Android App (Complete) - 100%
- ✅ Faculty Android App (Complete) - 100%
- 🚧 Testing & Deployment - 0%
- **GPS Geofencing** (30m radius validation)

### Key Components
1. **Backend Server** (Python + FastAPI + MySQL)
2. **Student Mobile App** (Kotlin Android - Native)
3. **Faculty Mobile App** (Kotlin Android - Native)
4. **SmartBoard Portal** (HTML5 + CSS3 + Vanilla JavaScript)

---

## 📁 Project Structure

```
IntelliAttend/
├── backend/                    # Python FastAPI Server
│   ├── app/
│   │   ├── api/v1/            # API endpoints (routers)
│   │   ├── core/              # Config, security, constants
│   │   ├── db/                # Database setup and session
│   │   ├── models/            # SQLAlchemy ORM models
│   │   ├── schemas/           # Pydantic request/response models
│   │   ├── services/          # Business logic
│   │   └── utils/             # Helper functions
│   ├── alembic/               # Database migrations
│   │   └── versions/          # Migration scripts
│   ├── scripts/               # Utility scripts (seed, init)
│   ├── tests/                 # pytest tests
│   │   ├── unit/
│   │   ├── integration/
│   │   └── api/
│   ├── main.py                # FastAPI app entry point
│   └── requirements.txt       # Python dependencies
│
├── mobile-student/            # Kotlin Android Student App
│   └── app/
│       └── src/
│           ├── main/java/com/intelliattend/student/
│           │   ├── ui/        # Screens, components (Compose)
│           │   ├── data/      # API, repository, local storage
│           │   ├── domain/    # Use cases, business models
│           │   ├── services/  # BLE, Wi-Fi, GPS, Prewarm
│           │   └── utils/     # Helpers
│           ├── test/          # Unit tests
│           └── androidTest/   # Instrumentation tests
│
├── mobile-faculty/            # Kotlin Android Faculty App
│   └── app/
│       └── src/
│           ├── main/java/com/intelliattend/faculty/
│           │   ├── ui/        # Screens, components
│           │   ├── data/      # API, repository
│           │   ├── services/  # WebSocket service
│           │   └── utils/
│           ├── test/
│           └── androidTest/
│
├── smartboard-portal/         # Vanilla JavaScript Web Portal
│   ├── js/                    # JavaScript modules
│   │   ├── main.js
│   │   ├── qr-renderer.js    # Canvas-based QR
│   │   ├── websocket-client.js
│   │   └── dashboard.js
│   ├── css/                   # Stylesheets
│   ├── assets/                # Images, icons
│   └── index.html             # Main HTML
│
├── shared/                    # Shared utilities
│   └── python/                # Python utility modules
│
├── docs/                      # Documentation
│   ├── api/                   # API reference
│   ├── architecture/          # System diagrams
│   └── guides/                # Setup, deployment
│
├── tests/                     # System-level tests
│   └── load/                  # Locust load tests
│
├── docker-compose.yml         # Development environment
├── .env.example               # Environment variables template
└── README.md                  # Project overview
```

---

## 🚀 Quick Start (Using Docker)

```bash
# 1. Clone the repository
git clone <repository-url>
cd IntelliAttend

# 2. Copy environment variables
cp .env.example .env
npm run dev
```

Server runs at `http://localhost:3000`

### 2. SmartBoard Portal Setup

```bash
cd smartboard-portal
npm install
npm run dev
```

Portal runs at `http://localhost:5173`

### 3. Mobile Apps Setup

```bash
# Student App
cd mobile-student
npm install
npx react-native run-android  # or run-ios

# Faculty App
cd mobile-faculty
npm install
npx react-native run-android  # or run-ios
```

---

## 📚 Documentation

- **[PRD](docs/PRD.md)**: Complete Product Requirements Document
- **[Implementation Plan](docs/IMPLEMENTATION_PLAN.md)**: Technical architecture and development phases
- **[API Reference](docs/api/)**: Complete API documentation
- **[Setup Guide](docs/SETUP.md)**: Detailed environment setup
- **[Testing Guide](docs/TESTING.md)**: Testing strategy and execution

---

## 🔐 Security Features

- **JWT Authentication** with secure token rotation
- **HTTPS/TLS Encryption** for all API calls
- **Biometric Verification** (Face ID/Touch ID) before QR scanning
- **Replay Attack Prevention** (7-second token expiration)
- **Multi-Factor Validation** (weighted confidence scoring)
- **Device Whitelisting** with unique device IDs
- **Geo-validation** with BLE UUID whitelisting

---

## 📊 Implementation Progress

| Component | Progress | Files | Status |
|-----------|----------|-------|--------|
| Backend Infrastructure | 100% | 50+ | ✅ Complete |
| Authentication System | 100% | 5 | ✅ Complete |
| Session Management | 100% | 3 | ✅ Complete |
| QR Token Service | 100% | 1 | ✅ Complete |
| Multi-Factor Verification | 100% | 1 | ✅ Complete |
| WebSocket System | 90% | 1 | ✅ Complete |
| SmartBoard Portal | 60% | 10 | ✅ Foundation Complete |
| Student App (Complete) | 100% | 30+ | ✅ Complete |
| Faculty App (Complete) | 100% | 20+ | ✅ Complete |

**Overall Progress: ~90%**
**Total Files Created: 150+**
**Lines of Code: ~10,000+**

---

## 🎯 Success Metrics

| KPI | Target |
|-----|--------|
| Attendance Accuracy | ≥ 99% |
| Session Completion Time | ≤ 2 minutes |
| Proxy Detection Rate | ≥ 95% |
| System Uptime | ≥ 99.5% |
| API Response Time | ≤ 200 ms |
| Concurrent Users | 5000+ |

---

## 📈 Development Phases

1. **Phase 1**: Core Infrastructure (Backend + Database) - Week 1-2
2. **Phase 2**: Session & QR Management - Week 3
3. **Phase 3**: Multi-Factor Verification Engine - Week 4-5
4. **Phase 4**: Student Mobile App - Week 6-8
5. **Phase 5**: Faculty App & SmartBoard Portal - Week 9-10
6. **Phase 6**: Testing & Deployment - Week 11-12

See [Implementation Plan](docs/IMPLEMENTATION_PLAN.md) for detailed breakdown.

---

## 🛠️ Technology Stack

### Backend
- **Framework**: Express.js (Node.js)
- **Database**: MySQL 8.0 with Sequelize ORM
- **Authentication**: JWT + bcrypt
- **Real-time**: Socket.IO
- **QR Generation**: qrcode npm package

### Mobile Apps
- **Framework**: React Native
- **Navigation**: React Navigation
- **State Management**: Context API + AsyncStorage
- **Sensors**: react-native-ble-manager, react-native-wifi-reborn, @react-native-community/geolocation
- **QR Scanner**: react-native-camera + react-native-qrcode-scanner
- **Biometric**: react-native-touch-id

### SmartBoard Portal
- **Framework**: React 18
- **Build Tool**: Vite
- **Real-time**: Socket.IO Client
- **QR Display**: qrcode.react with custom pixel animation

---

## 🧪 Testing

```bash
# Backend unit tests
cd backend && npm test

# Integration tests
npm run test:integration

# Load testing (5000+ concurrent users)
cd tests/load
artillery run attendance-submit.yml
```

---

## 📦 Deployment

### Local Network Deployment
```bash
# Build backend
cd backend && npm run build

# Build SmartBoard portal
cd smartboard-portal && npm run build

# Start with PM2
pm2 start ecosystem.config.js
```

### Docker Deployment
```bash
docker-compose -f docker-compose.prod.yml up -d
```

See [Deployment Guide](docs/architecture/deployment-guide.md) for detailed instructions.

---

## 🔮 Future Enhancements

- 🔹 **Campus Chat**: Encrypted messaging system
- 🔹 **Offline Mode**: Temporary data caching with sync
- 🔹 **AI Anomaly Detection**: Detect suspicious patterns
- 🔹 **Load Balancer**: High concurrency support (≥10,000 users)
- 🔹 **Cloudflare Tunnel**: Permanent domain endpoint

---

## 📄 License

© 2025 IntelliAttend. All rights reserved.

---

## 👥 Contributors

**Document Owner**: Bala Bhavani
**Version**: 1.0
**Last Updated**: December 2025

---

## 📞 Support

For issues, questions, or contributions, please refer to [CONTRIBUTING.md](docs/CONTRIBUTING.md).
