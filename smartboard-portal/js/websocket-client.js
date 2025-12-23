/**
 * WebSocket Client for real-time QR token updates
 * Handles connection to backend WebSocket for live session updates
 */

let websocket = null;
let reconnectAttempts = 0;
const MAX_RECONNECT_ATTEMPTS = 5;
const RECONNECT_DELAY = 3000; // 3 seconds

/**
 * Initialize WebSocket connection for a session
 * 
 * @param {string} sessionId - Session ID to connect to
 */
function initializeWebSocket(sessionId) {
    if (!sessionId) {
        console.error('[WebSocket] No session ID provided');
        return;
    }

    console.log('[WebSocket] Connecting to session:', sessionId);

    // Construct WebSocket URL
    const wsUrl = `${CONFIG.WS_URL}/ws/session/${sessionId}`;

    try {
        websocket = new WebSocket(wsUrl);

        websocket.onopen = handleWebSocketOpen;
        websocket.onmessage = handleWebSocketMessage;
        websocket.onerror = handleWebSocketError;
        websocket.onclose = handleWebSocketClose;

    } catch (error) {
        console.error('[WebSocket] Connection error:', error);
        attemptReconnect(sessionId);
    }
}

/**
 * Handle WebSocket connection opened
 */
function handleWebSocketOpen(event) {
    console.log('[WebSocket] ✅ Connected successfully');
    reconnectAttempts = 0;

    // Update connectivity indicator
    updateConnectivityStatus('server', true);

    // Request initial QR token
    requestQRToken();
}

/**
 * Handle incoming WebSocket messages
 */
function handleWebSocketMessage(event) {
    try {
        const data = JSON.parse(event.data);
        console.log('[WebSocket] Received:', data);

        // Handle different message types
        switch (data.type) {
            case 'qr_update':
                handleQRUpdate(data);
                break;

            case 'attendance_update':
                handleAttendanceUpdate(data);
                break;

            case 'session_status':
                handleSessionStatus(data);
                break;

            default:
                console.log('[WebSocket] Unknown message type:', data.type);
        }

    } catch (error) {
        console.error('[WebSocket] Message parse error:', error);
    }
}

/**
 * Handle QR token update
 */
function handleQRUpdate(data) {
    if (!data.qr_token) {
        console.error('[WebSocket] No QR token in update');
        return;
    }

    // Update QR code display
    if (typeof renderQRCode === 'function') {
        animateQRRefresh();
        renderQRCode(data.qr_token);
    }

    // Update sequence number
    if (data.sequence_number) {
        window.appState.qrSequence = data.sequence_number;
    }

    console.log('[WebSocket] QR updated - Seq:', data.sequence_number);
}

/**
 * Handle attendance update
 */
function handleAttendanceUpdate(data) {
    if (typeof updateDashboard === 'function') {
        // Pass the entire data object which may contain 'students' list
        updateDashboard(data);
    }

    console.log('[WebSocket] Attendance updated:', data.stats);
}

/**
 * Handle session status change
 */
function handleSessionStatus(data) {
    console.log('[WebSocket] Session status:', data.status);

    if (data.status === 'expired' || data.status === 'completed') {
        // Show session ended message
        alert(`Session ${data.status}. Returning to OTP screen.`);
        websocket.close();
        showScreen('otp-screen');
    }
}

/**
 * Handle WebSocket error
 */
function handleWebSocketError(error) {
    console.error('[WebSocket] Error:', error);
    updateConnectivityStatus('server', false);
}

/**
 * Handle WebSocket connection closed
 */
function handleWebSocketClose(event) {
    console.log('[WebSocket] Connection closed:', event.code, event.reason);
    updateConnectivityStatus('server', false);

    // Attempt reconnection if not intentional close
    if (event.code !== 1000 && reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
        attemptReconnect(window.appState.sessionId);
    }
}

/**
 * Attempt to reconnect WebSocket
 */
function attemptReconnect(sessionId) {
    reconnectAttempts++;
    console.log(`[WebSocket] Reconnect attempt ${reconnectAttempts}/${MAX_RECONNECT_ATTEMPTS}`);

    setTimeout(() => {
        initializeWebSocket(sessionId);
    }, RECONNECT_DELAY);
}

/**
 * Request QR token generation
 * (Placeholder for Phase 2 - will integrate with backend in Phase 3)
 */
function requestQRToken() {
    // TODO: Send message to server to generate QR token
    // For Phase 2, we'll simulate token updates

    console.log('[WebSocket] Requesting QR token...');

    // Simulate QR token updates every 5 seconds
    simulateQRUpdates();
}

/**
 * Simulate QR token updates (Phase 2 placeholder)
 */
function simulateQRUpdates() {
    let sequence = 0;

    const updateInterval = setInterval(() => {
        sequence++;

        const mockToken = `IATT_SESS123_${sequence}_${Date.now()}_ABC123`;

        handleQRUpdate({
            type: 'qr_update',
            qr_token: mockToken,
            sequence_number: sequence
        });

        // Stop after 24 updates (2 minutes for 5-second intervals)
        if (sequence >= 24) {
            clearInterval(updateInterval);
            handleSessionStatus({ status: 'completed' });
        }
    }, CONFIG.QR_REFRESH_INTERVAL);

    // Store interval ID to clear later
    window.appState.qrUpdateInterval = updateInterval;
}

/**
 * Close WebSocket connection
 */
function closeWebSocket() {
    if (websocket) {
        websocket.close(1000, 'Client closing connection');
        websocket = null;
    }

    // Clear any active intervals
    if (window.appState.qrUpdateInterval) {
        clearInterval(window.appState.qrUpdateInterval);
    }
}

console.log('🔌 WebSocket Client module loaded');
