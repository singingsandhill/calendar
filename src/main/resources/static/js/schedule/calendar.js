import { schedule, participants, selection, messages } from './state.js';

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

        const displayDay = currentDate.getDate();
        const displayMonth = currentDate.getMonth() + 1;
        const isOtherMonth = displayMonth !== schedule.month;
        const label = isOtherMonth ? `${displayMonth}/${displayDay}` : String(displayDay);

        const dayCell = createDayCell(index, label, isOtherMonth);
        decorateCell(dayCell, index);
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
        const dayCell = createDayCell(day, String(day), false);
        decorateCell(dayCell, day);
        calendarBody.appendChild(dayCell);
    }
}

function createDayCell(dayIndex, label, isOtherMonth) {
    const dayCell = document.createElement('div');
    dayCell.className = 'calendar-day';
    dayCell.dataset.day = dayIndex;
    dayCell.tabIndex = 0;
    dayCell.setAttribute('role', 'button');
    if (isOtherMonth) dayCell.classList.add('other-month');

    const dayNumber = document.createElement('span');
    dayNumber.className = 'day-number';
    dayNumber.textContent = label;
    dayCell.appendChild(dayNumber);

    dayCell.addEventListener('click', () => toggleDay(dayIndex, dayCell));
    dayCell.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            toggleDay(dayIndex, dayCell);
        }
    });
    return dayCell;
}

const HEAT_LEVELS = ['heat-1', 'heat-2', 'heat-3', 'heat-4', 'heat-5plus'];

function decorateCell(cell, dayIndex) {
    const available = participants.filter(p => p.selections && p.selections.includes(dayIndex));

    cell.classList.remove('solo', ...HEAT_LEVELS);
    cell.style.removeProperty('--solo-color');
    cell.querySelector('.participant-dots')?.remove();
    cell.querySelector('.cell-count-badge')?.remove();
    cell.querySelector('.cell-tooltip')?.remove();

    if (available.length === 1) {
        cell.classList.add('solo');
        cell.style.setProperty('--solo-color', available[0].color);
    } else if (available.length >= 5) {
        cell.classList.add('heat-5plus');
    } else if (available.length >= 2) {
        cell.classList.add('heat-' + available.length);
    }

    if (available.length >= 1) {
        const dots = document.createElement('div');
        dots.className = 'participant-dots';
        dots.setAttribute('aria-hidden', 'true');
        available.forEach(p => {
            const dot = document.createElement('span');
            dot.className = 'participant-dot';
            dot.style.backgroundColor = p.color;
            dots.appendChild(dot);
        });
        cell.appendChild(dots);
    }

    if (available.length >= 2) {
        const badge = document.createElement('span');
        badge.className = 'cell-count-badge';
        badge.setAttribute('aria-hidden', 'true');
        badge.textContent = available.length;
        cell.appendChild(badge);
    }

    const names = available.map(p => p.name);
    if (names.length > 0) {
        const tip = document.createElement('span');
        tip.className = 'cell-tooltip';
        tip.setAttribute('role', 'tooltip');
        tip.textContent = names.join(', ');
        cell.appendChild(tip);
        cell.setAttribute('aria-label', `${dayIndex}: ${names.length} available — ${names.join(', ')}`);
    } else {
        cell.setAttribute('aria-label', `${dayIndex}`);
    }
}

function toggleDay(day, cell) {
    if (!selection.currentParticipantId) {
        alert(messages.selectName);
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
        alert(messages.selectName);
        return;
    }
    selection.selectedDays.clear();
    updateCalendarDisplay();
}

export async function saveSelections() {
    if (!selection.currentParticipantId) {
        alert(messages.selectName);
        return;
    }

    try {
        await window.api.updateSelections(selection.currentParticipantId, Array.from(selection.selectedDays));
        const participant = participants.find(p => p.id === selection.currentParticipantId);
        if (participant) {
            participant.selections = Array.from(selection.selectedDays);
        }
        window.dataLayer = window.dataLayer || [];
        window.dataLayer.push({
            event: 'selections_saved',
            schedule_id: schedule.scheduleId,
            days_count: selection.selectedDays.size
        });
        renderCalendar();
        updateCalendarDisplay();
        window.toast.success(messages.saveSuccess);
    } catch (error) {
        window.toast.error(error.message);
    }
}
