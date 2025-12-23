# IntelliAttend Faculty Android App

Smart attendance session management app for faculty members.

## Features

- ✅ **Complete**: Login & Authentication
- ✅ **Complete**: Session Management
- ✅ **Complete**: Biometric Security (fingerprint/face/PIN)
- ✅ **Complete**: OTP Generation
- ✅ **Complete**: Live Dashboard with Real-time Updates
- ✅ **Complete**: Auto-polling (5s refresh)

## Architecture

- **Pattern**: MVVM (Model-View-ViewModel)
- **DI**: Hilt
- **UI**: Jetpack Compose + Material Design 3
- **Networking**: Retrofit + OkHttp
- **Storage**: DataStore (Preferences)
- **Navigation**: Jetpack Navigation Compose
- **Auth**: Biometric Prompt API

## Project Structure

```
app/src/main/java/com/intelliattend/faculty/
├── IntelliAttendApp.kt          # Application class
├── MainActivity.kt               # Main entry point
├── data/
│   ├── local/
│   │   └── PreferencesManager.kt # Token & profile storage
│   ├── model/
│   │   └── ApiModels.kt          # Data classes
│   ├── remote/
│   │   └── ApiService.kt         # Retrofit interface
│   └── repository/
│       ├── AuthRepository.kt     # Auth operations
│       └── SessionRepository.kt  # Session operations
├── di/
│   └── NetworkModule.kt          # Hilt DI modules
├── ui/
│   ├── auth/
│   │   ├── LoginScreen.kt        # Login UI
│   │   └── LoginViewModel.kt     # Login logic
│   ├── home/
│   │   └── HomeScreen.kt         # Dashboard UI
│   ├── session/
│   │   ├── SessionCreateScreen.kt # Session + Biometric
│   │   └── SessionViewModel.kt    # Session logic
│   ├── dashboard/
│   │   ├── LiveDashboardScreen.kt # Live stats
│   │   └── DashboardViewModel.kt  # Polling logic
│   └── theme/
│       ├── Color.kt, Theme.kt, Type.kt
└── utils/
    ├── Resource.kt               # State wrapper
    └── BiometricHelper.kt        # Biometric auth
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
   - Faculty ID: `FAC001`
   - Password: `demo123`

## Complete Flow

1. **Login** → JWT authentication
2. **Home** → "Start Attendance Session" button
3. **Biometric Auth** → Fingerprint/Face/PIN verification required
4. **Session Created** → OTP displayed (6-digit)
5. **OTP Entry** → Faculty enters OTP on SmartBoard
6. **Live Dashboard** → Auto-updates every 5 seconds
   - Present/Failed student counts
   - Attendance percentage
   - Student list with confidence scores
   - Real-time updates as students mark attendance

## API Endpoints Used

- `POST /api/v1/auth/login` - Faculty authentication
- `GET /api/v1/auth/me` - Profile
- `POST /api/v1/faculty/start_session` - Create session
- `POST /api/v1/faculty/generate_qr` - Generate QR (SmartBoard)
- `GET /api/v1/faculty/live_status/{sessionId}` - Live stats

## Security Features

- **Biometric Authentication**: Required before starting sessions
- **JWT Tokens**: Secure API communication
- **Encrypted Storage**: DataStore with encryption
- **OTP System**: 6-digit OTP with 5-minute expiry

## Dependencies

- Jetpack Compose BOM 2023.10.01
- Hilt 2.48.1
- Retrofit 2.9.0
- Biometric 1.1.0
- DataStore 1.0.0
- Navigation Compose 2.7.5

## Permissions

- **INTERNET** - API communication
- **USE_BIOMETRIC** - Biometric authentication
- **USE_FINGERPRINT** - Fingerprint support

## Build Info

- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Kotlin**: 1.9+
- **Java**: 17

## Next Steps

- Test biometric authentication
- Test session creation flow
- Test live dashboard polling
- Integration with SmartBoard
- End-to-end workflow validation
