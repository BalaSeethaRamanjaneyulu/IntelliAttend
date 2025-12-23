/**
 * Nexus ID "Living QR" Renderer - Working Version
 */

const qrCanvas = document.getElementById('qr-canvas');
const GRID_SIZE = 25;
const TRANSITION_TIME = 5000;
const FINDER_CELLS = 7;

let currentPattern = null;
let nextPattern = null;
let startTime = 0;
let isAnimating = false;

// Generate random pattern
function generatePattern() {
    const pattern = [];
    for (let y = 0; y < GRID_SIZE; y++) {
        pattern[y] = [];
        for (let x = 0; x < GRID_SIZE; x++) {
            const isTopLeft = x < FINDER_CELLS && y < FINDER_CELLS;
            const isTopRight = x >= GRID_SIZE - FINDER_CELLS && y < FINDER_CELLS;
            const isBottomLeft = x < FINDER_CELLS && y >= GRID_SIZE - FINDER_CELLS;
            const center = Math.floor(GRID_SIZE / 2);
            const isCenter = Math.abs(x - center) <= 2 && Math.abs(y - center) <= 2;

            if (isTopLeft || isTopRight || isBottomLeft || isCenter) {
                pattern[y][x] = 0;
            } else {
                pattern[y][x] = Math.random() > 0.65 ? 1 : 0;
            }
        }
    }
    return pattern;
}

// Draw rounded rectangle helper
function drawRoundedRect(ctx, x, y, w, h, r) {
    ctx.beginPath();
    ctx.moveTo(x + r, y);
    ctx.lineTo(x + w - r, y);
    ctx.quadraticCurveTo(x + w, y, x + w, y + r);
    ctx.lineTo(x + w, y + h - r);
    ctx.quadraticCurveTo(x + w, y + h, x + w - r, y + h);
    ctx.lineTo(x + r, y + h);
    ctx.quadraticCurveTo(x, y + h, x, y + h - r);
    ctx.lineTo(x, y + r);
    ctx.quadraticCurveTo(x, y, x + r, y);
    ctx.closePath();
}

// Draw finder pattern (corner squares)
function drawFinder(ctx, x, y, size, gradient) {
    const padding = size * 0.05;
    const outerSize = size - padding * 2;
    const innerSize = size * 0.42;
    const thickness = size * 0.14;
    const radius = size * 0.25;

    ctx.fillStyle = gradient;

    // Outer frame
    drawRoundedRect(ctx, x + padding, y + padding, outerSize, outerSize, radius);
    ctx.fill();

    // Clear inner hole
    ctx.save();
    ctx.globalCompositeOperation = 'destination-out';
    const holeSize = outerSize - thickness * 2;
    drawRoundedRect(ctx, x + padding + thickness, y + padding + thickness, holeSize, holeSize, radius * 0.6);
    ctx.fill();
    ctx.restore();

    // Inner solid core
    const coreOffset = (size - innerSize) / 2;
    ctx.fillStyle = gradient;
    drawRoundedRect(ctx, x + coreOffset, y + coreOffset, innerSize, innerSize, radius * 0.4);
    ctx.fill();
}

// Animation loop
function animate() {
    if (!currentPattern || !nextPattern || !qrCanvas) return;

    const ctx = qrCanvas.getContext('2d');
    const now = Date.now();
    const elapsed = now - startTime;
    let progress = (elapsed % TRANSITION_TIME) / TRANSITION_TIME;

    // Cycle patterns
    if (elapsed >= TRANSITION_TIME) {
        currentPattern = JSON.parse(JSON.stringify(nextPattern));
        nextPattern = generatePattern();
        startTime = now;
        progress = 0;
    }

    // Clear canvas
    ctx.clearRect(0, 0, 600, 600);

    const cellSize = 600 / GRID_SIZE;
    const dotRadius = cellSize * 0.28;

    // Create gradient
    const gradient = ctx.createLinearGradient(0, 0, 600, 600);
    gradient.addColorStop(0, '#00f2fe');
    gradient.addColorStop(1, '#4facfe');

    // Draw corner finders
    const finderSize = cellSize * FINDER_CELLS;
    drawFinder(ctx, 0, 0, finderSize, gradient);
    drawFinder(ctx, 600 - finderSize, 0, finderSize, gradient);
    drawFinder(ctx, 0, 600 - finderSize, finderSize, gradient);

    // Cosine easing
    const easedProgress = 0.5 - Math.cos(progress * Math.PI) / 2;

    // Draw dots
    for (let y = 0; y < GRID_SIZE; y++) {
        for (let x = 0; x < GRID_SIZE; x++) {
            const startVal = currentPattern[y][x];
            const endVal = nextPattern[y][x];
            const opacity = startVal + (endVal - startVal) * easedProgress;

            if (opacity > 0.01) {
                const cx = x * cellSize + cellSize / 2;
                const cy = y * cellSize + cellSize / 2;

                // Glow
                ctx.save();
                ctx.globalAlpha = opacity * 0.3;
                const glow = ctx.createRadialGradient(cx, cy, 0, cx, cy, dotRadius * 2);
                glow.addColorStop(0, '#00f2fe');
                glow.addColorStop(1, 'transparent');
                ctx.fillStyle = glow;
                ctx.beginPath();
                ctx.arc(cx, cy, dotRadius * 3, 0, Math.PI * 2);
                ctx.fill();
                ctx.restore();

                // Core dot
                ctx.save();
                ctx.globalAlpha = opacity;
                ctx.fillStyle = gradient;
                ctx.beginPath();
                ctx.arc(cx, cy, dotRadius, 0, Math.PI * 2);
                ctx.fill();
                ctx.restore();
            }
        }
    }

    requestAnimationFrame(animate);
}

// Main entry point
function renderQRCode(token) {
    if (!qrCanvas) {
        console.error('QR Canvas not found!');
        return;
    }

    console.log('🎨 Rendering Nexus ID:', token);

    if (!currentPattern) {
        // First time setup
        qrCanvas.width = 600;
        qrCanvas.height = 600;
        currentPattern = generatePattern();
        nextPattern = generatePattern();
        startTime = Date.now();

        if (!isAnimating) {
            isAnimating = true;
            animate();
        }
    } else {
        // Refresh pattern
        currentPattern = JSON.parse(JSON.stringify(nextPattern));
        nextPattern = generatePattern();
        startTime = Date.now();
    }
}

console.log('💎 Nexus ID Renderer Ready');
