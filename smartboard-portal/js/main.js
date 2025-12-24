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

    // OTP Verification Button Handler
    const verifyOtpBtn = document.getElementById('verify-otp-btn');
    if (verifyOtpBtn) {
        verifyOtpBtn.addEventListener('click', verifyOTPAndLinkSession);
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
    // Check if we have a valid session ID from OTP verification
    if (!window.appState.sessionId) {
        console.error('[Session] No session ID available. Please verify OTP first.');
        alert('Please enter a valid session code first');
        showScreen('otp-screen');
        return;
    }

    const startBtn = document.getElementById('start-session-btn');
    const timerDisplay = document.getElementById('session-timer-display');

    if (startBtn) startBtn.classList.add('hidden');
    if (timerDisplay) timerDisplay.classList.remove('hidden');

    window.appState.sessionStartTime = Date.now();

    // Start Clock Timer
    setInterval(updateTimer, 1000);

    // Initialize WebSocket connection - backend will push QR tokens every 5 seconds
    console.log('[Session] Connecting to WebSocket for session:', window.appState.sessionId);
    initializeWebSocket(window.appState.sessionId);

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

// QR tokens are now generated and pushed by the backend via WebSocket
// The qr_refresh_loop in websocket.py sends tokens every 5 seconds
// renderQRCode() is called by handleQRUpdate() in websocket-client.js
console.log('[QR] Backend WebSocket will push QR tokens every 5 seconds');

/**
 * Verify OTP and link SmartBoard to session
 */
async function verifyOTPAndLinkSession() {
    const otpInput = document.getElementById('otp-input');
    const otp = otpInput.value.trim();

    if (!otp) {
        alert('Please enter a session code');
        return;
    }

    // TEMPORARY: Bypass OTP for testing - use hardcoded session
    // Format matches backend: SESS_YYYYMMDDHHMMSS_ random
    console.log('[OTP] TESTING MODE: Using hardcoded session ID');
    window.appState.sessionId = 'SESS_20251224102700_ABC123';
    window.appState.isLinked = true;
    showScreen('dashboard-screen');
    initializeDashboard();
    console.log('[OTP] ✅ TEST MODE - Using session:', window.appState.sessionId);
    return;

    /* REAL IMPLEMENTATION (will enable once backend endpoint ready):
    try {
        console.log('[OTP] Verifying session code:', otp);

        // Call backend API to verify OTP 
        const response = await fetch(`${CONFIG.API_URL}/sessions/verify-otp`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ otp })
        });

        if (!response.ok) {
            throw new Error('Invalid session code');
        }

        const data = await response.json();
        console.log('[OTP] Verification successful:', data);

        // Store session ID
        window.appState.sessionId = data.session_id;
        window.appState.isLinked = true;

        // Show dashboard
        showScreen('dashboard-screen');
        initializeDashboard();

        console.log('[OTP] ✅ SmartBoard linked to session:', data.session_id);

    } catch (error) {
        console.error('[OTP] Verification failed:', error);
        alert('Invalid session code. Please try again.');
    }
    */
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

