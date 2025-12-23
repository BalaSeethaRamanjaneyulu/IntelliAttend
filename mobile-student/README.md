# IntelliAttend Student Android App

Smart attendance marking app with multi-factor verification.

## Features

- ✅ **Phase 1 Complete**: Login & Authentication
- 🚧 **Upcoming**: 
  - QR Code Scanner (CameraX + ML Kit)
  - BLE Beacon Scanning
  - GPS Location Tracking
  - WiFi BSSID Detection
  - Biometric Authentication
  - Background Warm Scan Service

## Architecture

- **Pattern**: MVVM (Model-View-ViewModel)
- **DI**: Hilt
- **UI**: Jetpack Compose + Material Design 3
- **Networking**: Retrofit + OkHttp
- **Storage**: DataStore (Preferences)
- **Navigation**: Jetpack Navigation Compose

## Project Structure

```
app/src/main/java/com/intelliattend/student/
├── IntelliAttendApp.kt          # Application class
├── MainActivity.kt               # Main entry point
├── data/
│   ├── local/
│   │   └── PreferencesManager.kt # Token & user data storage
│   ├── model/
│   │   └── ApiModels.kt          # Data classes for API
│   ├── remote/
│   │   └── ApiService.kt         # Retrofit API interface
│   └── repository/
│       └── AuthRepository.kt     # Auth data operations
├── di/
│   └── NetworkModule.kt          # Hilt DI modules
├── ui/
│   ├── auth/
│   │   ├── LoginScreen.kt        # Login UI
│   │   └─── LoginViewModel.kt     # Login logic
│   ├── home/
│   │   └── HomeScreen.kt         # Dashboard UI
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
└── utils/
    └── Resource.kt               # Network state wrapper
```

## Setup

1. **Configure API URL**
   - Edit `build.gradle.kts`
   - Change `API_BASE_URL` to your backend URL
   - For emulator: `http://10.0.2.2:8000/`
   - For device: `http://YOUR_IP:8000/`

2. **Build & Run**
   ```bash
   ./gradlew assembleDebug
   ./gradlew installDebug
   ```

3. **Demo Login**
   - Student ID: `STU001`, `STU002`, or `STU003`
   - Password: `demo123`

## Dependencies

- Jetpack Compose BOM 2023.10.01
- Hilt 2.48.1
- Retrofit 2.9.0
- CameraX 1.3.0
- ML Kit Barcode Scanning 17.2.0
- Biometric 1.1.0
- Play Services Location 21.0.1
- DataStore 1.0.0

## Permissions

The app requires the following permissions:
- 📷 **CAMERA** - QR code scanning
- 📍 **LOCATION** - GPS geofencing + BLE scanning
- 📡 **BLUETOOTH** - Beacon proximity detection
- 📶 **WIFI** - BSSID matching
- 🔐 **BIOMETRIC** - Fingerprint authentication

## API Endpoints Used

- `POST /api/v1/auth/login` - Student authentication
- `GET /api/v1/auth/me` - User profile
- `GET /api/v1/student/timetable` - Class schedule
- `POST /api/v1/attendance/submit` - Mark attendance

## Next Phase: Sensor Integration

- [ ] QR Scanner with CameraX
- [ ] BLE Beacon Scanner
- [ ] GPS Location Service
- [ ] WiFi Manager Integration
- [ ] Biometric Prompt
- [ ] Background Warm Scan Service
- [ ] Attendance Submission Flow

## Build Info

- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Kotlin**: 1.9+
- **Java**: 17
