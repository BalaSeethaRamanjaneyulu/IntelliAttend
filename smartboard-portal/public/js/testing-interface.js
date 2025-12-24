/**
 * IntelliAttend - Master Test Console Interface
 * Provides deep visibility into token rotation and session lifecycle
 */

const HOST = window.location.hostname || 'localhost';
const API_BASE_URL = `http://${HOST}:8000`;
const WS_BASE_URL = `ws://${HOST}:8000`;

let currentSession = null;
let websocket = null;
let countdownInterval = null;
let remainingSeconds = 5000;
let lastRotationTime = Date.now();
let previousTokenStr = null;

// DOM Elements
const startBtn = document.getElementById('startBtn');
const stopBtn = document.getElementById('stopBtn');
const sessionStatus = document.getElementById('sessionStatus');
const sessionIdEl = document.getElementById('sessionId');
const tokenSequence = document.getElementById('tokenSequence');
const tokenTimestamp = document.getElementById('tokenTimestamp');
const tokenExpiry = document.getElementById('tokenExpiry');
const timeSync = document.getElementById('timeSync');
const tokenPreview = document.getElementById('tokenPreview');
const previousTokenEl = document.getElementById('previousToken');
const rotationTimer = document.getElementById('rotationTimer');
const logContainer = document.getElementById('log-container');
const qrCanvas = document.getElementById('qr-canvas');

// Event Listeners
startBtn.addEventListener('click', startSession);
stopBtn.addEventListener('click', stopSession);

/**
 * Start session lifecycle
 */
async function startSession() {
    addLog('INFO', 'Initializing session request...');

    try {
        const response = await fetch(`${API_BASE_URL}/api/v1/sessions/create`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                faculty_id: 'DIAGNOSTIC_ADMIN',
                course_id: 'STABILIZATION_VAL_01'
            })
        });

        if (!response.ok) throw new Error(`HTTP_${response.status}: ${await response.text()}`);

        const data = await response.json();
        currentSession = data.session_id;

        // Update UI
        sessionIdEl.textContent = currentSession;
        sessionStatus.textContent = '● ACTIVE';
        sessionStatus.className = 'status-pill status-active';

        startBtn.disabled = true;
        stopBtn.disabled = false;

        addLog('SUCCESS', `Session linked: ${currentSession}`);
        addLog('INFO', `OTP bypass verification complete.`);

        connectWebSocket(currentSession);
        startTimer();

    } catch (error) {
        addLog('ERROR', `Initialization failed: ${error.message}`);
    }
}

/**
 * Terminate session lifecycle
 */
async function stopSession() {
    if (!currentSession) return;
    addLog('INFO', 'Sending termination signal...');

    try {
        await fetch(`${API_BASE_URL}/api/v1/sessions/${currentSession}/end`, { method: 'POST' });
        addLog('SUCCESS', 'Server-side session terminated.');
    } catch (e) {
        addLog('ERROR', `Cleanup sync failed: ${e.message}`);
    } finally {
        resetUI();
    }
}

/**
 * WebSocket Uplink
 */
function connectWebSocket(sid) {
    const wsUrl = `${WS_BASE_URL}/api/v1/websocket/session/${sid}`;
    addLog('INFO', `Establishing WebSocket uplink to ${wsUrl}`);

    websocket = new WebSocket(wsUrl);

    websocket.onopen = () => addLog('SUCCESS', 'Uplink established. Waiting for first rotation...');

    websocket.onmessage = (event) => {
        const data = JSON.parse(event.data);
        if (data.type === 'qr_update') {
            handleUpdate(data);
        } else if (data.type === 'attendance_update') {
            addLog('SUCCESS', `Attendance sync: ${data.total_present} verified.`);
        }
    };

    websocket.onerror = () => addLog('ERROR', 'WebSocket uplink failure.');
    websocket.onclose = () => {
        addLog('INFO', 'WebSocket uplink closed.');
        if (sessionStatus.textContent.includes('ACTIVE')) {
            addLog('ERROR', 'Unexpected disconnection. API might be down.');
        }
    };
}

/**
 * Handle Real-time Update
 */
function handleUpdate(data) {
    const { qr_token, sequence_number, timestamp, expiry } = data;

    // Store previous
    if (tokenPreview.textContent !== '待機中...') {
        previousTokenStr = tokenPreview.textContent;
        previousTokenEl.textContent = previousTokenStr;
    }

    // Update labels
    tokenSequence.textContent = String(sequence_number).padStart(5, '0');
    tokenTimestamp.textContent = new Date(timestamp * 1000).toLocaleTimeString();
    tokenExpiry.textContent = new Date(expiry * 1000).toLocaleTimeString();
    tokenPreview.textContent = qr_token;

    // Calculate sync offset
    const now = Date.now() / 1000;
    const offset = now - timestamp;
    timeSync.textContent = `${offset.toFixed(3)}s`;

    // Render
    renderQR(qr_token);

    addLog('SUCCESS', `Rotation #${sequence_number} received. Latency: ${(offset * 1000).toFixed(0)}ms`);

    // Reset timer
    lastRotationTime = Date.now();
}

/**
 * QR Rendering - Using qrcode.js library correctly
 */
function renderQR(token) {
    const ctx = qrCanvas.getContext('2d');

    // Clear canvas and fill white
    ctx.fillStyle = 'white';
    ctx.fillRect(0, 0, qrCanvas.width, qrCanvas.height);

    try {
        // Create temporary container for QR generation
        const tempDiv = document.createElement('div');
        tempDiv.style.display = 'none';
        document.body.appendChild(tempDiv);

        // Generate QR code using library
        const qrcode = new QRCode(tempDiv, {
            text: token,
            width: 320,
            height: 320,
            colorDark: "#1e293b",
            colorLight: "#ffffff",
            correctLevel: QRCode.CorrectLevel.M
        });

        // Wait for image generation then draw to canvas
        setTimeout(() => {
            const img = tempDiv.querySelector('img');
            if (img) {
                if (img.complete) {
                    ctx.drawImage(img, 0, 0, 320, 320);
                } else {
                    img.onload = () => {
                        ctx.drawImage(img, 0, 0, 320, 320);
                    };
                }
            }
            // Clean up temporary element
            document.body.removeChild(tempDiv);
        }, 150);

    } catch (e) {
        // Error fallback
        ctx.fillStyle = '#1e293b';
        ctx.fillRect(0, 0, 320, 320);
        ctx.fillStyle = '#f43f5e';
        ctx.font = 'bold 14px monospace';
        ctx.textAlign = 'center';
        ctx.fillText('QR Error', 160, 150);
        ctx.font = '10px monospace';
        ctx.fillText(e.message, 160, 170);
        addLog('ERROR', `QR render error: ${e.message}`);
        console.error('QR Error:', e);
    }
}

/**
 * Timer Engine
 */
function startTimer() {
    if (countdownInterval) clearInterval(countdownInterval);
    countdownInterval = setInterval(() => {
        const elapsed = Date.now() - lastRotationTime;
        const remaining = Math.max(0, 5000 - elapsed);
        rotationTimer.textContent = (remaining / 1000).toFixed(2);
    }, 50);
}

/**
 * Log Engine
 */
function addLog(tag, msg) {
    const entry = document.createElement('div');
    entry.className = 'log-entry';
    const tagClass = `tag-${tag.toLowerCase()}`;

    entry.innerHTML = `
        <span class="log-time">[${new Date().toLocaleTimeString()}]</span>
        <span class="log-tag ${tagClass}">${tag}</span>
        <span>${msg}</span>
    `;

    logContainer.prepend(entry);

    // Keep last 50
    while (logContainer.children.length > 50) {
        logContainer.removeChild(logContainer.lastChild);
    }
}

/**
 * UI Reset
 */
function resetUI() {
    if (websocket) websocket.close();
    if (countdownInterval) clearInterval(countdownInterval);

    currentSession = null;
    sessionStatus.textContent = '● OFFLINE';
    sessionStatus.className = 'status-pill status-idle';
    sessionIdEl.textContent = 'NOT_STARTED';
    tokenSequence.textContent = '00000';
    tokenTimestamp.textContent = '--:--:--';
    tokenExpiry.textContent = '--:--:--';
    tokenPreview.textContent = '待機中...';
    previousTokenEl.textContent = 'N/A';
    rotationTimer.textContent = '05.00';

    startBtn.disabled = false;
    stopBtn.disabled = true;

    const ctx = qrCanvas.getContext('2d');
    ctx.clearRect(0, 0, qrCanvas.width, qrCanvas.height);

    addLog('INFO', 'System state reset successfully.');
}
