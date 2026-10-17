import { schedule, messages } from './state.js';

// 일정 설명 편집 (ADR datedate/domain/0011) — 본문은 서버가 그리고, 저장하면 새로 고쳐 서버 렌더를 다시 받는다.

function elements() {
    return {
        view: document.getElementById('descriptionView'),
        form: document.getElementById('descriptionForm'),
        input: document.getElementById('descriptionInput'),
        count: document.getElementById('descriptionCount')
    };
}

function updateCount() {
    const { input, count } = elements();
    count.textContent = messages.descriptionCountTemplate
        .replace('{0}', input.value.length)
        .replace('{1}', input.maxLength);
}

export function initDescription() {
    const { input } = elements();
    if (!input) return;
    input.addEventListener('input', updateCount);
    updateCount();
}

export function openDescriptionEditor() {
    const { view, form, input } = elements();
    view.hidden = true;
    form.hidden = false;
    updateCount();
    input.focus();
}

export function closeDescriptionEditor() {
    const { view, form, input } = elements();
    input.value = input.defaultValue;
    form.hidden = true;
    view.hidden = false;
    view.querySelector('[data-action="description.edit"]')?.focus();
}

export async function saveDescription() {
    const { input } = elements();
    try {
        await window.api.updateScheduleDescription(schedule.ownerId, schedule.year, schedule.month, input.value);
        window.dataLayer = window.dataLayer || [];
        window.dataLayer.push({
            event: 'description_saved',
            schedule_id: schedule.scheduleId,
            description_length: input.value.trim().length
        });
        window.location.reload();
    } catch (error) {
        window.toast.error(error.message);
    }
}
