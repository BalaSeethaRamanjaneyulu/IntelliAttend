/**
 * Main application initialization
 * Phase 1: Basic structure - Full implementation in Phase 2-3
 */

console.log('🎓 IntelliAttend SmartBoard Portal - Version 1.0.0');
console.log('Backend API:', CONFIG.API_URL);

// Global state
window.appState = {
    sessionId: null,
    isLinked: false,
    wsConnection: null,
    sessionStartTime: null,
    qrInterval: null
};

// Initialize on DOM load
document.addEventListener('DOMContentLoaded', () => {
    console.log('✅ SmartBoard Portal initialized');
    showScreen('otp-screen');
    updateConnectivityStatus();
    checkViewportSize();

    // Start Session Button Handler
    const startBtn = document.getElementById('start-session-btn');
    if (startBtn) {
        startBtn.addEventListener('click', startSessionSequence);
    }

    // Fullscreen Prompt Handlers
    const enterFullscreenBtn = document.getElementById('enter-fullscreen-btn');
    const dismissPromptBtn = document.getElementById('dismiss-prompt-btn');

    if (enterFullscreenBtn) {
        enterFullscreenBtn.addEventListener('click', enterFullscreen);
    }

    if (dismissPromptBtn) {
        dismissPromptBtn.addEventListener('click', dismissPrompt);
    }
});

function startSessionSequence() {
    const startBtn = document.getElementById('start-session-btn');
    const timerDisplay = document.getElementById('session-timer-display');

    if (startBtn) startBtn.classList.add('hidden');
    if (timerDisplay) timerDisplay.classList.remove('hidden');

    window.appState.sessionStartTime = Date.now();

    // Start Clock Timer
    setInterval(updateTimer, 1000);

    // Initial QR
    generateNextQR();

    // 5-Second Refresh Loop
    window.appState.qrInterval = setInterval(generateNextQR, 5000);

    // Mock Attendance Simulation (Phase 2 Testing)
    simulateAttendanceUpdates();
}

function simulateAttendanceUpdates() {
    const mockStudents = [];
    for (let i = 0; i < 50; i++) {
        mockStudents.push({ student_id: 1000 + i, status: 'absent' });
    }

    let presentCount = 0;
    const interval = setInterval(() => {
        if (presentCount >= 30) {
            clearInterval(interval);
            return;
        }

        const randomIndex = Math.floor(Math.random() * 50);
        const rand = Math.random();
        const randomStatus = rand > 0.4 ? 'present' : (rand > 0.15 ? 'pending' : 'failed');

        mockStudents[randomIndex].status = randomStatus;
        if (typeof updateDashboard === 'function') {
            updateDashboard({ students: mockStudents });
        }

        presentCount++;
    }, 2000);
}

function updateTimer() {
    if (!window.appState.sessionStartTime) return;
    const elapsed = Math.floor((Date.now() - window.appState.sessionStartTime) / 1000);
    const mins = Math.floor(elapsed / 60).toString().padStart(2, '0');
    const secs = (elapsed % 60).toString().padStart(2, '0');
    document.getElementById('timer-display').textContent = `${mins}:${secs}`;
}

function generateNextQR() {
    // Generate a secure mock session token
    const mockToken = `IATT_${Math.random().toString(36).substring(7)}_${Date.now()}`;
    if (typeof renderQRCode === 'function') {
        renderQRCode(mockToken);
    }
}

function showScreen(screenId) {
    // Hide all screens
    document.querySelectorAll('.screen').forEach(screen => {
        screen.classList.remove('active');
    });

    // Show selected screen
    const screen = document.getElementById(screenId);
    if (screen) {
        screen.classList.add('active');
    }
}

function updateConnectivityStatus() {
    const wifiStatus = document.getElementById('wifi-status');
    const bleStatus = document.getElementById('ble-status');
    const serverStatus = document.getElementById('server-status');

    wifiStatus.classList.add('active');
    serverStatus.classList.add('active');
}

/**
 * Check viewport size and show fullscreen prompt if too small
 */
function checkViewportSize() {
    const minWidth = 1024;
    const minHeight = 600;
    const viewportWidth = window.innerWidth;
    const viewportHeight = window.innerHeight;

    const prompt = document.getElementById('fullscreen-prompt');
    if (!prompt) return;

    // Show prompt if viewport is smaller than minimum recommended size
    if (viewportWidth < minWidth || viewportHeight < minHeight) {
        // Check if user has dismissed prompt in this session
        if (!sessionStorage.getItem('fullscreen-prompt-dismissed')) {
            prompt.classList.remove('hidden');
        }
    } else {
        prompt.classList.add('hidden');
    }
}

/**
 * Enter fullscreen mode
 */
function enterFullscreen() {
    const elem = document.documentElement;

    if (elem.requestFullscreen) {
        elem.requestFullscreen();
    } else if (elem.webkitRequestFullscreen) { /* Safari */
        elem.webkitRequestFullscreen();
    } else if (elem.msRequestFullscreen) { /* IE11 */
        elem.msRequestFullscreen();
    }

    // Hide the prompt
    dismissPrompt();
}

/**
 * Dismiss the fullscreen prompt
 */
function dismissPrompt() {
    const prompt = document.getElementById('fullscreen-prompt');
    if (prompt) {
        prompt.classList.add('hidden');
        sessionStorage.setItem('fullscreen-prompt-dismissed', 'true');
    }
}

// Monitor window resize and check viewport size
window.addEventListener('resize', () => {
    checkViewportSize();
});

// Monitor fullscreen changes
document.addEventListener('fullscreenchange', () => {
    if (document.fullscreenElement) {
        dismissPrompt();
    }
});

