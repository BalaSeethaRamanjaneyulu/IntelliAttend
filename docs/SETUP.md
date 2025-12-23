# IntelliAttend Setup Guide

This guide walks you through setting up the IntelliAttend development environment on your local machine.

---

## 📋 Prerequisites

Before you begin, ensure you have the following installed:

### Required Software
- **Node.js** 18.x or higher ([Download](https://nodejs.org/))
- **npm** 9.x or higher (comes with Node.js)
- **MySQL** 8.0 or higher ([Download](https://dev.mysql.com/downloads/mysql/))
- **Git** ([Download](https://git-scm.com/downloads))

### Optional (Recommended)
- **Docker** & **Docker Compose** ([Download](https://www.docker.com/products/docker-desktop))
- **Postman** or **Insomnia** for API testing

### For Mobile Development
- **React Native CLI**: `npm install -g react-native-cli`
- **iOS Development** (macOS only):
  - Xcode 14+ from Mac App Store
  - CocoaPods: `sudo gem install cocoapods`
- **Android Development**:
  - Android Studio with Android SDK
  - Java Development Kit (JDK) 11

---

## 🚀 Quick Start (Using Docker)

The fastest way to get started is using Docker Compose:

```bash
# 1. Clone the repository
git clone <repository-url>
cd IntelliAttend

# 2. Copy environment variables
cp .env.example .env

# 3. Start services
docker-compose up -d

# 4. Verify services are running
docker-compose ps
```

This will start:
- MySQL database on `localhost:3306`
- Backend API on `localhost:3000`
- SmartBoard portal on `localhost:5173`

**Access Points**:
- Backend API: http://localhost:3000
- SmartBoard Portal: http://localhost:5173
- MySQL: `localhost:3306` (user: `intelliattend_user`, password: see `.env`)

---

## 🛠️ Manual Setup (Without Docker)

### 1. Clone Repository

```bash
git clone <repository-url>
cd IntelliAttend
```

### 2. Database Setup

#### Option A: Install MySQL Locally
```bash
# macOS (using Homebrew)
brew install mysql
brew services start mysql

# Ubuntu/Debian
sudo apt-get install mysql-server
sudo systemctl start mysql

# Create database and user
mysql -u root -p
```

```sql
CREATE DATABASE intelliattend;
CREATE USER 'intelliattend_user'@'localhost' IDENTIFIED BY 'your_secure_password';
GRANT ALL PRIVILEGES ON intelliattend.* TO 'intelliattend_user'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

#### Option B: Use Docker for MySQL Only
```bash
docker run --name intelliattend-mysql \
  -e MYSQL_ROOT_PASSWORD=root_password \
  -e MYSQL_DATABASE=intelliattend \
  -e MYSQL_USER=intelliattend_user \
  -e MYSQL_PASSWORD=your_secure_password \
  -p 3306:3306 \
  -d mysql:8.0
```

### 3. Backend Setup

```bash
cd backend

# Install dependencies
npm install

# Configure environment variables
cp .env.example .env
# Edit .env with your database credentials

# Run database migrations
npm run migrate

# Seed demo data (optional but recommended)
npm run seed

# Start development server
npm run dev
```

**Expected output**:
```
Server running on http://localhost:3000
Database connected successfully
```

**Test backend**:
```bash
curl http://localhost:3000/health
# Should return: {"status":"ok","timestamp":"..."}
```

### 4. SmartBoard Portal Setup

```bash
cd smartboard-portal

# Install dependencies
npm install

# Configure environment variables
cp .env.example .env

# Start development server
npm run dev
```

**Expected output**:
```
  VITE v4.x.x  ready in xxx ms

  ➜  Local:   http://localhost:5173/
  ➜  Network: use --host to expose
```

**Test SmartBoard**: Open http://localhost:5173 in your browser

### 5. Student Mobile App Setup

```bash
cd mobile-student

# Install dependencies
npm install

# iOS: Install CocoaPods dependencies
cd ios && pod install && cd ..

# Configure environment variables
cp .env.example .env
# UPDATE: Set REACT_APP_API_URL to your local IP (e.g., http://192.168.1.100:3000)
```

#### Run on iOS (macOS only)
```bash
npx react-native run-ios
# Or specify device:
npx react-native run-ios --simulator="iPhone 14 Pro"
```

#### Run on Android
```bash
# Start Android emulator first (via Android Studio)
# Or connect physical device with USB debugging enabled

npx react-native run-android
```

**Troubleshooting Mobile**:
- **Metro bundler not starting**: Run `npx react-native start` in a separate terminal
- **CocoaPods error**: Run `cd ios && pod deintegrate && pod install`
- **Android build fails**: Ensure `ANDROID_HOME` is set correctly

### 6. Faculty Mobile App Setup

```bash
cd mobile-faculty

# Install dependencies
npm install

# iOS: Install CocoaPods dependencies
cd ios && pod install && cd ..

# Configure environment variables
cp .env.example .env

# Run on iOS or Android
npx react-native run-ios
# or
npx react-native run-android
```

---

## ⚙️ Environment Configuration

### Backend `.env`
```env
NODE_ENV=development
PORT=3000

DB_HOST=localhost
DB_PORT=3306
DB_NAME=intelliattend
DB_USER=intelliattend_user
DB_PASSWORD=your_secure_password

JWT_SECRET=your_super_secret_jwt_key_at_least_32_chars
JWT_EXPIRES_IN=24h

# See .env.example for more options
```

### SmartBoard Portal `.env`
```env
VITE_API_URL=http://localhost:3000
VITE_WS_URL=ws://localhost:3000
```

### Mobile Apps `.env`
```env
# IMPORTANT: Use your computer's local IP, not localhost
# Find your IP: ipconfig (Windows) or ifconfig (macOS/Linux)
REACT_APP_API_URL=http://192.168.1.100:3000
REACT_APP_WS_URL=ws://192.168.1.100:3000
```

**How to find your local IP**:
```bash
# macOS
ipconfig getifaddr en0

# Linux
hostname -I | awk '{print $1}'

# Windows
ipconfig
# Look for "IPv4 Address" under your active network adapter
```

---

## 🧪 Verify Installation

### 1. Backend Health Check
```bash
curl http://localhost:3000/health
```
Expected: `{"status":"ok"}`

### 2. Database Connection
```bash
cd backend
npm run db:test
```
Expected: "Database connection successful"

### 3. Login Test (Demo Credentials)
```bash
curl -X POST http://localhost:3000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"demo_student","password":"demo123"}'
```
Expected: JWT token in response

### 4. SmartBoard Portal
- Open http://localhost:5173
- Should see OTP entry screen

### 5. Mobile Apps
- Launch app in simulator/emulator
- Should see login screen
- Try logging in with demo credentials:
  - **Student**: username: `demo_student`, password: `demo123`
  - **Faculty**: username: `demo_faculty`, password: `demo123`

---

## 📦 Development Scripts

### Backend
```bash
npm run dev          # Start development server (auto-reload)
npm run start        # Start production server
npm run migrate      # Run database migrations
npm run migrate:undo # Rollback last migration
npm run seed         # Seed database with demo data
npm test             # Run all tests
npm run test:unit    # Run unit tests
npm run lint         # Check code style
npm run lint:fix     # Auto-fix code style issues
```

### SmartBoard Portal
```bash
npm run dev          # Start development server
npm run build        # Build for production
npm run preview      # Preview production build
npm run lint         # Check code style
```

### Mobile Apps
```bash
npm start            # Start Metro bundler
npm test             # Run tests
npm run android      # Run on Android
npm run ios          # Run on iOS
```

---

## 🐛 Common Issues

### Backend won't start
- **Error: Connection refused (MySQL)**
  - Ensure MySQL is running: `brew services list` (macOS) or `sudo systemctl status mysql` (Linux)
  - Check credentials in `.env` match your MySQL setup
  - Test connection: `mysql -u intelliattend_user -p intelliattend`

- **Error: Port 3000 already in use**
  - Kill process: `lsof -ti:3000 | xargs kill -9`
  - Or change PORT in `.env`

### SmartBoard Portal won't start
- **Error: Port 5173 already in use**
  - Kill process: `lsof -ti:5173 | xargs kill -9`
  - Or change port in `vite.config.js`

### Mobile App Issues
- **iOS: Command PhaseScriptExecution failed**
  - Clean build: `cd ios && xcodebuild clean && cd ..`
  - Reinstall pods: `cd ios && rm -rf Pods Podfile.lock && pod install`

- **Android: SDK location not found**
  - Create `local.properties` in `android/`:
    ```
    sdk.dir=/Users/YOUR_USERNAME/Library/Android/sdk
    ```

- **Metro Bundler: Unable to resolve module**
  - Clear cache: `npx react-native start --reset-cache`
  - Reinstall: `rm -rf node_modules && npm install`

### Database Migration Errors
- **Error: Table already exists**
  - Rollback: `npm run migrate:undo`
  - Or drop database and recreate

---

## 🎯 Next Steps

Once your environment is set up:

1. **Read the Documentation**:
   - [Project Structure](PROJECT_STRUCTURE.md)
   - [API Documentation](api/)
   - [Contributing Guidelines](CONTRIBUTING.md)

2. **Explore the PRD**:
   - [Product Requirements Document](../PRD.md)

3. **Start Development**:
   - Pick a task from the Implementation Plan
   - Create a feature branch
   - Start coding!

4. **Join the Community**:
   - Report issues on GitHub
   - Submit pull requests
   - Participate in discussions

---

## 📞 Need Help?

- **Documentation**: Check [docs/](.)
- **Issues**: Search [GitHub Issues](https://github.com/your-org/IntelliAttend/issues)
- **Questions**: Open a [GitHub Discussion](https://github.com/your-org/IntelliAttend/discussions)

---

Happy coding! 🚀
