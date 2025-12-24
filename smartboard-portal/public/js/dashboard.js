/**
 * Dashboard functionality for live attendance statistics (Seating Grid Focus)
 */

const ROWS = ['A', 'B', 'C', 'D', 'E'];
const COLS = 10;
const TOTAL_SEATS = 50;

/**
 * Update dashboard with attendance data
 */
function updateDashboard(data) {
    if (!data) return;

    // Handle student updates
    if (data.students) {
        renderStudentGrid(data.students);
    } else if (data.student_update) {
        // Individual update logic could go here to avoid full rerender
        updateIndividualSeat(data.student_update);
    }
}

/**
 * Render student seating grid (BookMyShow style)
 */
function renderStudentGrid(students) {
    const gridContainer = document.getElementById('student-grid');
    if (!gridContainer) return;

    gridContainer.innerHTML = '';

    // Wrap the grid to add row labels
    for (let r = 0; r < ROWS.length; r++) {
        const rowLabel = ROWS[r];

        // Add row label element
        const labelDiv = document.createElement('div');
        labelDiv.className = 'grid-row-label';
        labelDiv.textContent = rowLabel;
        gridContainer.appendChild(labelDiv);

        // Add 10 seats for this row
        for (let c = 0; c < COLS; c++) {
            const i = r * COLS + c;
            const student = students[i];
            const colIdx = c + 1;

            const seat = document.createElement('div');
            seat.className = `seat ${student ? student.status : 'absent'}`;
            seat.id = `seat-${i}`;

            // Content: Show seat number or ID
            const displayId = student ? (student.student_id ? student.student_id.toString().slice(-2) : colIdx) : colIdx;
            seat.textContent = displayId;

            // Tooltip
            seat.title = student ? `${student.name || 'Student'} (${student.status})` : `Seat ${rowLabel}${colIdx}`;

            gridContainer.appendChild(seat);
        }
    }
}

/**
 * Update a single seat status without rerendering the whole grid
 */
function updateIndividualSeat(update) {
    // This assumes the update contains an index or verifiable ID
    // For now, let's keep it simple with renderStudentGrid
}

/**
 * Initialize dashboard with default state
 */
function initializeDashboard() {
    const mockStudents = [];
    for (let i = 0; i < TOTAL_SEATS; i++) {
        mockStudents.push({
            student_id: 1000 + i,
            status: 'absent'
        });
    }
    renderStudentGrid(mockStudents);
}

// Initial render
document.addEventListener('DOMContentLoaded', () => {
    if (document.getElementById('dashboard-screen').classList.contains('active')) {
        initializeDashboard();
    }
});

console.log('📈 Seating Grid module initialized');

