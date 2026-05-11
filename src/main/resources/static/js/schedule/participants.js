import { schedule, participants, selection } from './state.js';
import { updateCalendarDisplay } from './calendar.js';

export function onParticipantChange() {
    const select = document.getElementById('participantSelect');
    selection.currentParticipantId = select.value ? parseInt(select.value) : null;

    selection.selectedDays.clear();
    const calendarBody = document.getElementById('calendarBody');
    if (selection.currentParticipantId) {
        const participant = participants.find(p => p.id === selection.currentParticipantId);
        if (participant && participant.selections) {
            participant.selections.forEach(day => selection.selectedDays.add(day));
        }
        if (participant && calendarBody) {
            calendarBody.style.setProperty('--current-color', participant.color);
        }
    } else if (calendarBody) {
        calendarBody.style.removeProperty('--current-color');
    }

    updateCalendarDisplay();
}

export function openAddParticipantModal() {
    document.getElementById('addParticipantModal').classList.add('show');
}

export function closeAddParticipantModal() {
    document.getElementById('addParticipantModal').classList.remove('show');
}

async function handleAddParticipant(e) {
    e.preventDefault();
    const name = document.getElementById('participantName').value.trim();

    try {
        await window.api.addParticipant(schedule.scheduleId, name);
        window.dataLayer = window.dataLayer || [];
        window.dataLayer.push({
            event: 'participant_added',
            schedule_id: schedule.scheduleId,
            participant_count_after: participants.length + 1
        });
        window.location.reload();
    } catch (error) {
        window.toast.error(error.message);
    }
}

export function bindParticipantForm() {
    const form = document.getElementById('addParticipantForm');
    if (form) {
        form.addEventListener('submit', handleAddParticipant);
    }

    const select = document.getElementById('participantSelect');
    if (select) {
        select.addEventListener('change', onParticipantChange);
    }
}
