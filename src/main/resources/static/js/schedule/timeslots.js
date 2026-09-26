import { schedule, participants, timeSlots, messages, formatVotes } from './state.js';
import { escapeHtml, dayIndexToDate } from './utils.js';
import { getSelectedVoterName, toggleVoteFor } from './voting.js';

// 서버 TimeSlot 검증과 같은 단위: 30분 칸, 00:00~24:00 (자정 넘김 없음)
const SLOT_MINUTES = 30;
const MINUTES_PER_DAY = 24 * 60;
const DEFAULT_START = 18 * 60;
const DEFAULT_END = 20 * 60;
// 차트 축이 이보다 길면 시각 눈금을 2시간 간격으로 줄인다 (모바일 폭)
const DENSE_AXIS_HOURS = 12;

const weekdayFormat = new Intl.DateTimeFormat(document.documentElement.lang || 'ko', { weekday: 'short' });

function formatMinute(minute) {
    const hours = String(Math.floor(minute / 60)).padStart(2, '0');
    const minutes = String(minute % 60).padStart(2, '0');
    return `${hours}:${minutes}`;
}

function formatRange(start, end) {
    return `${formatMinute(start)}–${formatMinute(end)}`;
}

function formatDay(dayIndex) {
    const date = dayIndexToDate(dayIndex);
    return `${date.getMonth() + 1}/${date.getDate()} (${weekdayFormat.format(date)})`;
}

function sameName(a, b) {
    return a.toLowerCase() === b.toLowerCase();
}

function sortedSlots() {
    return [...timeSlots].sort((a, b) =>
        a.dayIndex - b.dayIndex || a.startMinute - b.startMinute || a.endMinute - b.endMinute);
}

// ==================== Form ====================

// 참여자가 저장한 날의 합집합 → [dayIndex, 가능 인원] (날짜순)
function availableDayCounts() {
    const counts = new Map();
    participants.forEach(p => {
        (p.selections || []).forEach(day => counts.set(day, (counts.get(day) || 0) + 1));
    });
    return [...counts.entries()].sort((a, b) => a[0] - b[0]);
}

export function refreshDayOptions() {
    const select = document.getElementById('timeDaySelect');
    if (!select) return;
    const previous = select.value;
    const days = availableDayCounts();

    select.innerHTML = '';
    if (days.length === 0) {
        select.appendChild(new Option(messages.timeDayEmpty, ''));
        select.disabled = true;
        return;
    }
    select.disabled = false;
    days.forEach(([day, count]) => {
        const label = `${formatDay(day)} · ${messages.timeDayCountTemplate.replace('{0}', count)}`;
        select.appendChild(new Option(label, String(day)));
    });
    if (days.some(([day]) => String(day) === previous)) {
        select.value = previous;
    }
}

function fillTimeSelect(select, from, to, selected) {
    for (let minute = from; minute <= to; minute += SLOT_MINUTES) {
        select.appendChild(new Option(formatMinute(minute), String(minute)));
    }
    select.value = String(selected);
}

export function initTimeSlots() {
    const startSelect = document.getElementById('timeStartSelect');
    const endSelect = document.getElementById('timeEndSelect');
    if (!startSelect || !endSelect) return;
    fillTimeSelect(startSelect, 0, MINUTES_PER_DAY - SLOT_MINUTES, DEFAULT_START);
    fillTimeSelect(endSelect, SLOT_MINUTES, MINUTES_PER_DAY, DEFAULT_END);
    refreshDayOptions();
    renderTimeSlots();
}

export async function addTimeSlot() {
    const dayIndex = parseInt(document.getElementById('timeDaySelect').value, 10);
    if (!dayIndex) {
        alert(messages.timeDayEmpty);
        return;
    }
    const startMinute = parseInt(document.getElementById('timeStartSelect').value, 10);
    const endMinute = parseInt(document.getElementById('timeEndSelect').value, 10);
    if (endMinute <= startMinute) {
        alert(messages.timeRangeInvalid);
        return;
    }
    try {
        const newTimeSlot = await window.api.addTimeSlot(schedule.scheduleId, dayIndex, startMinute, endMinute);
        timeSlots.push(newTimeSlot);
        renderTimeSlots();
        window.dataLayer = window.dataLayer || [];
        window.dataLayer.push({
            event: 'time_slot_added',
            schedule_id: schedule.scheduleId,
            time_slot_count_after: timeSlots.length
        });
    } catch (error) {
        window.toast.error(error.message);
    }
}

export async function voteTimeSlot(button) {
    const timeSlotId = parseInt(button.dataset.timeSlotId, 10);
    const voterName = getSelectedVoterName();
    if (!voterName) return;

    const timeSlot = timeSlots.find(t => t.id === timeSlotId);
    if (!timeSlot) return;

    const wasVoted = timeSlot.voters.some(v => sameName(v, voterName));

    try {
        await toggleVoteFor(
            timeSlot,
            voterName,
            window.api.voteTimeSlot.bind(window.api),
            window.api.unvoteTimeSlot.bind(window.api)
        );
        window.dataLayer = window.dataLayer || [];
        window.dataLayer.push({
            event: 'vote_cast',
            target: 'time',
            target_id: timeSlotId,
            action: wasVoted ? 'unvote' : 'vote'
        });
        renderTimeSlots();
        // 목록을 다시 그리므로 키보드 사용자의 포커스를 같은 버튼으로 되돌린다
        document.querySelector(`#timeSlotList [data-time-slot-id="${timeSlotId}"]`)?.focus();
    } catch (error) {
        window.toast.error(error.message);
    }
}

// ==================== Render ====================

export function renderTimeSlots() {
    const slots = sortedSlots();
    renderList(slots);
    renderOverlap(slots);
}

function renderList(slots) {
    const list = document.getElementById('timeSlotList');
    if (!list) return;
    if (slots.length === 0) {
        list.innerHTML = `<p class="empty-locations">${escapeHtml(messages.timeEmpty)}</p>`;
        return;
    }
    list.innerHTML = slots.map(slot => `
        <div class="location-item">
            <div class="location-info">
                <span class="location-name">${escapeHtml(formatDay(slot.dayIndex))} ${formatRange(slot.startMinute, slot.endMinute)}</span>
                <span class="location-votes">${escapeHtml(formatVotes(slot.voteCount))}</span>
            </div>
            <div class="location-voters">${slot.voters.map(v =>
                `<span class="voter-tag">${escapeHtml(v)}</span>`).join('')}</div>
            <div class="location-actions">
                <button class="btn btn-sm btn-primary"
                        data-action="time.vote"
                        data-time-slot-id="${slot.id}">${escapeHtml(messages.voteLabel)}</button>
            </div>
        </div>`).join('');
}

// 칸마다 "그 칸을 덮는 후보에 투표한 서로 다른 사람" 목록 (대소문자 무시)
function votersPerCell(daySlots, axisStart, cellCount) {
    const cells = Array.from({ length: cellCount }, () => []);
    daySlots.forEach(slot => {
        for (let minute = slot.startMinute; minute < slot.endMinute; minute += SLOT_MINUTES) {
            const names = cells[(minute - axisStart) / SLOT_MINUTES];
            slot.voters.forEach(v => {
                if (!names.some(n => sameName(n, v))) names.push(v);
            });
        }
    });
    return cells;
}

function sameVoters(a, b) {
    return a.length === b.length && a.every(name => b.some(other => sameName(name, other)));
}

// 최대 인원 칸들을 "같은 사람들"끼리 연속 구간으로 묶는다
function bestWindows(cells, axisStart) {
    const max = Math.max(0, ...cells.map(names => names.length));
    const windows = [];
    if (max === 0) return { max, windows };
    for (let i = 0; i < cells.length; i++) {
        if (cells[i].length !== max) continue;
        let end = i;
        while (end + 1 < cells.length && sameVoters(cells[end + 1], cells[i])) end++;
        windows.push({
            startCell: i,
            endCell: end,
            start: axisStart + i * SLOT_MINUTES,
            end: axisStart + (end + 1) * SLOT_MINUTES,
            names: cells[i]
        });
        i = end;
    }
    return { max, windows };
}

function heatLevel(count) {
    return count >= 5 ? 'lv-5plus' : `lv-${count}`;
}

function renderOverlap(slots) {
    const container = document.getElementById('timeOverlap');
    const chart = document.getElementById('timeOverlapChart');
    if (!container || !chart) return;
    chart.innerHTML = '';
    if (slots.length === 0) {
        container.hidden = true;
        return;
    }
    container.hidden = false;

    // 모든 줄이 같은 축을 공유해 날짜 간 비교가 되도록, 전체 후보 범위를 정시 단위로 넓힌다
    const axisStart = Math.floor(Math.min(...slots.map(s => s.startMinute)) / 60) * 60;
    const axisEnd = Math.ceil(Math.max(...slots.map(s => s.endMinute)) / 60) * 60;
    const cellCount = (axisEnd - axisStart) / SLOT_MINUTES;
    chart.style.setProperty('--time-cells', cellCount);

    chart.appendChild(renderAxis(axisStart, axisEnd));
    const days = [...new Set(slots.map(s => s.dayIndex))];
    days.forEach(day => {
        const daySlots = slots.filter(s => s.dayIndex === day);
        chart.appendChild(renderDayRow(day, votersPerCell(daySlots, axisStart, cellCount), axisStart));
    });
}

function renderAxis(axisStart, axisEnd) {
    const row = document.createElement('div');
    row.className = 'time-row time-axis-row';
    row.setAttribute('aria-hidden', 'true');
    row.appendChild(document.createElement('span'));

    const axis = document.createElement('div');
    axis.className = 'time-axis';
    const hours = (axisEnd - axisStart) / 60;
    for (let h = 0; h < hours; h++) {
        const tick = document.createElement('span');
        tick.className = 'time-tick';
        if (hours <= DENSE_AXIS_HOURS || h % 2 === 0) {
            tick.textContent = String(axisStart / 60 + h);
        }
        axis.appendChild(tick);
    }
    row.appendChild(axis);
    return row;
}

function renderDayRow(dayIndex, cells, axisStart) {
    const { max, windows } = bestWindows(cells, axisStart);

    const row = document.createElement('div');
    row.className = 'time-row';

    const label = document.createElement('span');
    label.className = 'time-row-label';
    label.textContent = formatDay(dayIndex);
    row.appendChild(label);

    const strip = document.createElement('div');
    strip.className = 'time-strip';
    strip.setAttribute('aria-hidden', 'true');
    cells.forEach((names, i) => {
        const cell = document.createElement('span');
        cell.className = `time-cell ${heatLevel(names.length)}`;
        if (windows.some(w => i >= w.startCell && i <= w.endCell)) {
            cell.classList.add('best');
        }
        const start = axisStart + i * SLOT_MINUTES;
        const range = formatRange(start, start + SLOT_MINUTES);
        cell.title = names.length > 0 ? `${range} · ${names.join(', ')}` : range;
        strip.appendChild(cell);
    });
    row.appendChild(strip);

    const summary = document.createElement('p');
    summary.className = 'time-row-best';
    if (max === 0) {
        summary.textContent = messages.timeNoVotes;
        summary.classList.add('empty');
    } else {
        const ranges = windows.map(w => formatRange(w.start, w.end)).join(', ');
        const names = windows.length === 1 ? ` (${windows[0].names.join(', ')})` : '';
        summary.textContent = messages.timeBestTemplate
            .replace('{0}', ranges)
            .replace('{1}', max) + names;
    }
    row.appendChild(summary);
    return row;
}
