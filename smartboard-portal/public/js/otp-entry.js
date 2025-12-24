/**
 * OTP Entry functionality for SmartBoard
 */

const otpInput = document.getElementById('otp-input');
const otpSubmitBtn = document.getElementById('otp-submit');
const otpError = document.getElementById('otp-error');

// Handle OTP input - only allow digits
if (otpInput) {
    otpInput.addEventListener('input', (e) => {
        // Remove non-digit characters
        e.target.value = e.target.value.replace(/\D/g, '');

        // Clear error when user types
        otpError.textContent = '';

        // Enable submit button only when 6 digits entered
        if (e.target.value.length === 6) {
            otpSubmitBtn.disabled = false;
            otpSubmitBtn.classList.add('active');
        } else {
            otpSubmitBtn.disabled = true;
            otpSubmitBtn.classList.remove('active');
        }
    });

    // Auto-submit on Enter key
    otpInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter' && otpInput.value.length === 6) {
            submitOTP();
        }
    });
}

// Handle OTP submission
if (otpSubmitBtn) {
    otpSubmitBtn.addEventListener('click', submitOTP);
}

async function submitOTP() {
    const otp = otpInput.value.trim();

    if (otp.length !== 6) {
        showError('Please enter a 6-digit OTP');
        return;
    }

    // Show loading state
    otpSubmitBtn.disabled = true;
    otpSubmitBtn.textContent = 'Verifying...';

    try {
        // First, we need a session_id - in real implementation, this would come from scanning
        // For Phase 2, we'll use a placeholder or get it from the faculty app
        // TODO: Implement proper session discovery mechanism

        // Temporary: Generate QR token with placeholder session
        // In production, this would:
        // 1. Get session_id from faculty (QR code or manual entry)
        // 2. Send OTP + session_id to server
        // 3. Server validates and returns first QR token
        // 4. Start WebSocket for continuous QR updates

        console.log('[OTP Entry] Submitted OTP:', otp);

        // For now, show success and transition to QR screen
        // Real implementation in Phase 3 will connect to backend
        showError('✅ OTP verified! Connecting to session...');

        setTimeout(() => {
            // Store OTP for generate_qr call
            window.appState.currentOTP = otp;

            // Transition to QR screen
            showScreen('qr-screen');

            // Initialize WebSocket and start QR generation
            if (typeof initializeWebSocket === 'function') {
                // Will be implemented in websocket-client.js
                console.log('[OTP Entry] Ready to initialize WebSocket');
            }
        }, 1000);

    } catch (error) {
        console.error('[OTP Entry] Error:', error);
        showError('Failed to verify OTP. Please try again.');
        otpSubmitBtn.disabled = false;
        otpSubmitBtn.textContent = 'Link Session';
    }
}

function showError(message) {
    otpError.textContent = message;
    if (message.startsWith('✅')) {
        otpError.style.color = '#4caf50';
    } else {
        otpError.style.color = '#f44336';
    }
}

console.log('📱 OTP Entry module loaded');
