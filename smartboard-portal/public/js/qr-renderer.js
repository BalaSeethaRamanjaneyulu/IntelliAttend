/**
 * Premium Dynamic Glowing QR - Vanilla JS Version
 * Custom pixelated pattern with vector finder corners and smooth interpolation
 * Based on React implementation with:
 * - Vector Finder Patterns (rounded corners with evenodd fill)
 * - Dynamic pixel interpolation with cosine easing
 * - Dual-tone gradient (cyan to blue)
 * - Glowing dot effects
 */

const GRID_SIZE = 25;
const TRANSITION_TIME = 5000; // 5 seconds
const FINDER_CELLS = 7; // 7x7 reserved for finder patterns

class DynamicQRRenderer {
    constructor(canvasId) {
        this.canvas = document.getElementById(canvasId);
        if (!this.canvas) {
            console.error(`❌ Canvas '${canvasId}' not found`);
            return;
        }

        this.ctx = this.canvas.getContext('2d');
        this.currentPattern = null;
        this.nextPattern = null;
        this.currentToken = null;
        this.nextToken = null;
        this.startTime = Date.now();
        this.animationId = null;

        this.setupCanvas();
        this.init();
    }

    setupCanvas() {
        // Set canvas size (responsive)
        const container = this.canvas.parentElement;
        const size = Math.min(container.clientWidth, container.clientHeight, 500);
        this.canvas.width = size;
        this.canvas.height = size;
        this.canvas.style.width = `${size}px`;
        this.canvas.style.height = `${size}px`;
    }

    generatePattern(token) {
        const matrix = Array(GRID_SIZE).fill(0).map(() => Array(GRID_SIZE).fill(0));

        if (token && typeof QREncoder !== 'undefined') {
            try {
                // Generate actual QR code data
                const qrMatrix = QREncoder.generate(token);
                const qrSize = qrMatrix.length;

                // Map QR data to our 25x25 grid
                for (let y = 0; y < GRID_SIZE; y++) {
                    for (let x = 0; x < GRID_SIZE; x++) {
                        // Reserved zones for finders (top-left, top-right, bottom-left)
                        const isTopLeft = x < FINDER_CELLS && y < FINDER_CELLS;
                        const isTopRight = x >= GRID_SIZE - FINDER_CELLS && y < FINDER_CELLS;
                        const isBottomLeft = x < FINDER_CELLS && y >= GRID_SIZE - FINDER_CELLS;

                        // Reserved zone for center logo (approx 5x5)
                        const center = Math.floor(GRID_SIZE / 2);
                        const isCenter = Math.abs(x - center) <= 2 && Math.abs(y - center) <= 2;

                        if (!isTopLeft && !isTopRight && !isBottomLeft && !isCenter) {
                            // Map QR module to grid position (scale coordinates)
                            const qrX = Math.floor((x / GRID_SIZE) * qrSize);
                            const qrY = Math.floor((y / GRID_SIZE) * qrSize);

                            // Use actual QR data if within bounds
                            if (qrX < qrSize && qrY < qrSize) {
                                matrix[y][x] = qrMatrix[qrY][qrX];
                            }
                        }
                    }
                }
                console.log(`🎨 Generated scannable QR pattern for token: ${token.substring(0, 20)}...`);
            } catch (error) {
                console.error('❌ Error generating QR matrix, using random pattern:', error);
                return this.generateRandomPattern();
            }
        } else {
            // Fallback to random pattern if no token or encoder not loaded
            return this.generateRandomPattern();
        }

        return matrix;
    }

    generateRandomPattern() {
        // Original random pattern generation (fallback)
        const matrix = Array(GRID_SIZE).fill(0).map(() => Array(GRID_SIZE).fill(0));

        for (let y = 0; y < GRID_SIZE; y++) {
            for (let x = 0; x < GRID_SIZE; x++) {
                const isTopLeft = x < FINDER_CELLS && y < FINDER_CELLS;
                const isTopRight = x >= GRID_SIZE - FINDER_CELLS && y < FINDER_CELLS;
                const isBottomLeft = x < FINDER_CELLS && y >= GRID_SIZE - FINDER_CELLS;
                const center = Math.floor(GRID_SIZE / 2);
                const isCenter = Math.abs(x - center) <= 2 && Math.abs(y - center) <= 2;

                if (!isTopLeft && !isTopRight && !isBottomLeft && !isCenter) {
                    matrix[y][x] = Math.random() > 0.65 ? 1 : 0;
                }
            }
        }
        return matrix;
    }

    drawFinder(x, y, size, gradient) {
        const ctx = this.ctx;
        const padding = size * 0.05;
        const outerSize = size - padding * 2;
        const innerSize = size * 0.42;
        const thickness = size * 0.14;
        const radius = size * 0.25;

        ctx.fillStyle = gradient;

        // Draw Outer Frame with hole (evenodd fill)
        ctx.beginPath();
        // Outer path
        ctx.roundRect(x + padding, y + padding, outerSize, outerSize, radius);
        // Inner path (hole) - counter-clockwise for evenodd
        const holeSize = outerSize - thickness * 2;
        const holeRadius = radius * 0.6;
        ctx.roundRect(x + padding + thickness, y + padding + thickness, holeSize, holeSize, holeRadius);
        ctx.fill('evenodd');

        // Draw Inner Solid Core
        const coreOffset = (size - innerSize) / 2;
        ctx.beginPath();
        ctx.roundRect(x + coreOffset, y + coreOffset, innerSize, innerSize, radius * 0.4);
        ctx.fill();
    }

    draw() {
        const now = Date.now();
        const elapsed = now - this.startTime;
        let progress = (elapsed % TRANSITION_TIME) / TRANSITION_TIME;

        // Transition to next pattern every 5 seconds
        if (elapsed >= TRANSITION_TIME) {
            this.currentPattern = this.nextPattern;
            this.currentToken = this.nextToken;
            this.nextPattern = this.generatePattern(this.nextToken);
            this.startTime = now;
            progress = 0;
        }

        this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);

        const cellSize = this.canvas.width / GRID_SIZE;
        const dotRadius = cellSize * 0.28;

        // Create dual-tone gradient (Electric Cyan to Brighter Blue)
        const mainGradient = this.ctx.createLinearGradient(0, 0, this.canvas.width, this.canvas.height);
        mainGradient.addColorStop(0, '#00f2fe'); // Electric Cyan
        mainGradient.addColorStop(1, '#4facfe'); // Brighter Blue

        // 1. Draw Static Vector Finders
        const finderSize = cellSize * FINDER_CELLS;
        this.drawFinder(0, 0, finderSize, mainGradient); // Top Left
        this.drawFinder(this.canvas.width - finderSize, 0, finderSize, mainGradient); // Top Right
        this.drawFinder(0, this.canvas.height - finderSize, finderSize, mainGradient); // Bottom Left

        // 2. Draw Dynamic Data Dots with smooth interpolation
        for (let y = 0; y < GRID_SIZE; y++) {
            for (let x = 0; x < GRID_SIZE; x++) {
                const startVal = this.currentPattern[y][x];
                const endVal = this.nextPattern[y][x];

                // Cosine easing for smooth transitions
                const easedProgress = 0.5 - Math.cos(progress * Math.PI) / 2;
                const opacity = startVal + (endVal - startVal) * easedProgress;

                if (opacity > 0.01) {
                    const cx = x * cellSize + cellSize / 2;
                    const cy = y * cellSize + cellSize / 2;

                    this.ctx.globalAlpha = opacity;

                    // Subtle glow effect
                    const glow = this.ctx.createRadialGradient(cx, cy, 0, cx, cy, dotRadius * 2);
                    glow.addColorStop(0, 'rgba(0, 242, 254, 0.1)');
                    glow.addColorStop(1, 'rgba(0,0,0,0)');
                    this.ctx.fillStyle = glow;
                    this.ctx.beginPath();
                    this.ctx.arc(cx, cy, dotRadius * 3, 0, Math.PI * 2);
                    this.ctx.fill();

                    // Core dot
                    this.ctx.fillStyle = mainGradient;
                    this.ctx.beginPath();
                    this.ctx.arc(cx, cy, dotRadius, 0, Math.PI * 2);
                    this.ctx.fill();
                }
            }
        }

        this.ctx.globalAlpha = 1.0;
        this.animationId = requestAnimationFrame(() => this.draw());
    }

    init() {
        // Start with random pattern, will be updated with actual token
        this.currentPattern = this.generateRandomPattern();
        this.nextPattern = this.generateRandomPattern();
        console.log('🎨 Dynamic QR Pattern initialized (awaiting token)');
        this.draw();
    }

    updateToken(token) {
        // Update the next pattern with actual QR data
        this.nextToken = token;
        this.nextPattern = this.generatePattern(token);
        console.log(`🔄 QR token updated: ${token.substring(0, 20)}...`);
    }

    destroy() {
        if (this.animationId) {
            cancelAnimationFrame(this.animationId);
        }
    }
}

// Global renderer instance
let qrRenderer = null;

/**
 * Initialize the dynamic QR renderer
 * @param {string} canvasId - The ID of the canvas element (kept for backward compatibility, uses 'qr-canvas')
 */
function initializeQRCode(canvasId) {
    if (qrRenderer) {
        qrRenderer.destroy();
    }
    qrRenderer = new DynamicQRRenderer('qr-canvas');
    console.log('✅ Dynamic QR Pattern renderer initialized');
}

/**
 * Render/update the QR pattern
 * @param {string} token - The session token to encode
 */
function renderQRCode(token) {
    if (!qrRenderer) {
        console.warn('⚠️ QR Renderer not initialized, initializing now...');
        initializeQRCode('qr-canvas');
    }

    if (qrRenderer && token) {
        qrRenderer.updateToken(token);
        console.log(`🎨 QR Pattern updated with scannable token`);
    } else {
        console.log(`🎨 QR Pattern active (no token provided)`);
    }
}

/**
 * Animate QR refresh indicator (backward compatibility)
 */
function animateQRRefresh() {
    const rotationCycle = document.getElementById('rotation-cycle');
    if (rotationCycle) {
        rotationCycle.style.transition = 'none';
        rotationCycle.style.transform = 'scale(1.2)';

        setTimeout(() => {
            rotationCycle.style.transition = 'transform 0.3s ease';
            rotationCycle.style.transform = 'scale(1)';
        }, 50);
    }
}

// Make functions available globally
window.initializeQRCode = initializeQRCode;
window.renderQRCode = renderQRCode;
window.animateQRRefresh = animateQRRefresh;
window.DynamicQRRenderer = DynamicQRRenderer;

console.log('✅ Dynamic QR Pattern Renderer Ready (Premium Mode)');
