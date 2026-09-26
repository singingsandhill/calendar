import { messages, schedule } from './state.js';

export function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// calendar.js 의 셀 렌더링과 같은 규칙 — 확장 모드는 그리드 시작(1일이 속한 주의 일요일)부터의
// 1-based 인덱스, 레거시 모드는 해당 월의 일자.
export function dayIndexToDate(dayIndex) {
    if (schedule.isExtendedMode) {
        return new Date(schedule.year, schedule.month - 1, 1 - schedule.firstDayOfWeek + (dayIndex - 1));
    }
    return new Date(schedule.year, schedule.month - 1, dayIndex);
}

export function copyLink() {
    const url = window.location.href;
    if (navigator.clipboard && window.isSecureContext) {
        navigator.clipboard.writeText(url)
            .then(() => {
                pushLinkShared('clipboard');
                alert(messages.linkCopied);
            })
            .catch(() => fallbackCopy(url));
    } else {
        fallbackCopy(url);
    }
}

function fallbackCopy(text) {
    const textarea = document.createElement('textarea');
    textarea.value = text;
    textarea.style.position = 'fixed';
    textarea.style.opacity = '0';
    document.body.appendChild(textarea);
    textarea.select();
    try {
        document.execCommand('copy');
        pushLinkShared('execCommand');
        alert(messages.linkCopied);
    } catch (err) {
        prompt(messages.linkCopyPrompt, text);
    }
    document.body.removeChild(textarea);
}

function pushLinkShared(method) {
    window.dataLayer = window.dataLayer || [];
    window.dataLayer.push({
        event: 'link_shared',
        schedule_id: schedule.scheduleId,
        share_method: method
    });
}
