/**
 * Configuration for SmartBoard Portal
 * Update API_URL to match your backend server address
 */

const CONFIG = {
    // Backend API URL (change for production)
    API_URL: 'http://localhost:8000',
    
    // WebSocket URL
    WS_URL: 'ws://localhost:8000',
    
    // QR Refresh interval (milliseconds)
    QR_REFRESH_INTERVAL: 5000,
    
    // Session duration (minutes)
    SESSION_DURATION: 2
};

// Export for use in other modules
if (typeof module !== 'undefined' && module.exports) {
    module.exports = CONFIG;
}
