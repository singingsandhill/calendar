import { schedule, participants, selection } from './state.js';

export function renderCalendar() {
    const calendarBody = document.getElementById('calendarBody');
    calendarBody.innerHTML = '';

    if (schedule.isExtendedMode) {
        renderExtendedCalendar(calendarBody);
    } else {
        renderLegacyCalendar(calendarBody);
    }
}

function renderExtendedCalendar(calendarBody) {
    const firstOfMonth = new Date(schedule.year, schedule.month - 1, 1);
    const gridStart = new Date(firstOfMonth);
    gridStart.setDate(gridStart.getDate() - schedule.firstDayOfWeek);

    for (let index = 1; index <= schedule.totalDays; index++) {
        const currentDate = new Date(gridStart);
        currentDate.setDate(gridStart.getDate() + (index - 1));

        const dayCell = document.createElement('div');
        dayCell.className = 'calendar-day';
        dayCell.dataset.day = index;

        const dayNumber = document.createElement('span');
        dayNumber.className = 'day-number';

        const displayDay = currentDate.getDate();
        const displayMonth = currentDate.getMonth() + 1;

        if (displayMonth !== schedule.month) {
            dayCell.classList.add('other-month');
            dayNumber.textContent = `${displayMonth}/${displayDay}`;
        } else {
            dayNumber.textContent = displayDay;
        }

        dayCell.appendChild(dayNumber);
        appendDots(dayCell, index);
        dayCell.addEventListener('click', () => toggleDay(index, dayCell));
        calendarBody.appendChild(dayCell);
    }
}

function renderLegacyCalendar(calendarBody) {
    for (let i = 0; i < schedule.firstDayOfWeek; i++) {
        const emptyCell = document.createElement('div');
        emptyCell.className = 'calendar-day empty';
        calendarBody.appendChild(emptyCell);
    }

    for (let day = 1; day <= schedule.daysInMonth; day++) {
        const dayCell = document.createElement('div');
        dayCell.className = 'calendar-day';
        dayCell.dataset.day = day;

        const dayNumber = document.createElement('span');
        dayNumber.className = 'day-number';
        dayNumber.textContent = day;
        dayCell.appendChild(dayNumber);

        appendDots(dayCell, day);
        dayCell.addEventListener('click', () => toggleDay(day, dayCell));
        calendarBody.appendChild(dayCell);
    }
}

function appendDots(dayCell, dayIndex) {
    const dotsContainer = document.createElement('div');
    dotsContainer.className = 'participant-dots';

    participants.forEach(p => {
        if (p.selections && p.selections.includes(dayIndex)) {
            const dot = document.createElement('span');
            dot.className = 'participant-dot';
            dot.style.backgroundColor = p.color;
            dot.title = p.name;
            dotsContainer.appendChild(dot);
        }
    });

    dayCell.appendChild(dotsContainer);
}

function toggleDay(day, cell) {
    if (!selection.currentParticipantId) {
        alert('먼저 이름을 선택하세요');
        return;
    }

    if (selection.selectedDays.has(day)) {
        selection.selectedDays.delete(day);
        cell.classList.remove('selected');
    } else {
        selection.selectedDays.add(day);
        cell.classList.add('selected');
    }
}

export function updateCalendarDisplay() {
    document.querySelectorAll('.calendar-day').forEach(cell => {
        cell.classList.remove('selected');
        const day = parseInt(cell.dataset.day);
        if (selection.selectedDays.has(day)) {
            cell.classList.add('selected');
        }
    });
}

export function resetSelections() {
    if (!selection.currentParticipantId) {
        alert('먼저 이름을 선택하세요');
        return;
    }
    selection.selectedDays.clear();
    updateCalendarDisplay();
}

export async function saveSelections() {
    if (!selection.currentParticipantId) {
        alert('먼저 이름을 선택하세요');
        return;
    }

    try {
        await window.api.updateSelections(selection.currentParticipantId, Array.from(selection.selectedDays));
        const participant = participants.find(p => p.id === selection.currentParticipantId);
        if (participant) {
            participant.selections = Array.from(selection.selectedDays);
        }
        renderCalendar();
        updateCalendarDisplay();
        window.toast.success('저장되었습니다!');
    } catch (error) {
        window.toast.error(error.message);
    }
}
