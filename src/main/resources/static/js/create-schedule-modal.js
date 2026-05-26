/**
 * create-schedule-modal.js
 * Owner 대시보드 / Schedule 뷰 공통 "새 일정" 모달.
 *
 * 의존: api.js (createSchedule), toast.js
 * ownerId: window.OWNER_DATA?.ownerId 또는 window.SCHEDULE_DATA?.ownerId
 *
 * 헤더의 [data-create-cta] 링크는 모달이 DOM에 존재할 때만 hijack 한다.
 * 모달이 없는 페이지(홈, 마케팅 등)에서는 기존 href(/#start-form)대로 동작.
 */
(function () {
    'use strict';

    function getOwnerId() {
        return window.OWNER_DATA?.ownerId ?? window.SCHEDULE_DATA?.ownerId ?? null;
    }

    function resetYearMonthToNow() {
        const now = new Date();
        const currentYear = now.getFullYear();
        const currentMonth = now.getMonth() + 1;

        const yearSelect = document.getElementById('year');
        if (yearSelect) {
            yearSelect.replaceChildren();
            for (let y = currentYear; y <= currentYear + 5; y++) {
                const opt = document.createElement('option');
                opt.value = String(y);
                opt.textContent = String(y);
                yearSelect.appendChild(opt);
            }
            yearSelect.value = String(currentYear);
        }

        const monthSelect = document.getElementById('month');
        if (monthSelect) {
            monthSelect.value = String(currentMonth);
        }
    }

    function clearError() {
        const errorEl = document.getElementById('createFormError');
        if (errorEl) {
            errorEl.style.display = 'none';
            errorEl.textContent = '';
        }
    }

    window.openCreateModal = function () {
        const modal = document.getElementById('createModal');
        if (!modal) return;
        clearError();
        resetYearMonthToNow();
        modal.classList.add('show');
        document.getElementById('year')?.focus();
    };

    window.closeCreateModal = function () {
        document.getElementById('createModal')?.classList.remove('show');
    };

    async function handleCreateSchedule(e) {
        e.preventDefault();
        clearError();

        const ownerId = getOwnerId();
        if (!ownerId) return;

        const year = parseInt(document.getElementById('year').value, 10);
        const month = parseInt(document.getElementById('month').value, 10);

        try {
            await api.createSchedule(ownerId, year, month);
            window.DDAnalytics?.markOwnedSchedule(ownerId);
            window.dataLayer = window.dataLayer || [];
            window.dataLayer.push({ event: 'schedule_created', owner_id: ownerId, year, month });
            const target = `/${ownerId}/${year}/${month}`;
            window.location.href = window.localeUrl ? window.localeUrl(target) : target;
        } catch (error) {
            const errorEl = document.getElementById('createFormError');
            if (errorEl) {
                errorEl.textContent = error.message;
                errorEl.style.display = 'block';
            }
            toast.error(error.message);
        }
    }

    document.addEventListener('DOMContentLoaded', () => {
        const modal = document.getElementById('createModal');

        // 모달이 없는 페이지에서는 헤더 CTA를 건드리지 않는다 (기본 href 동작).
        if (!modal) return;

        document.getElementById('createScheduleForm')
            ?.addEventListener('submit', handleCreateSchedule);

        modal.querySelectorAll('[data-create-modal-close]').forEach(btn => {
            btn.addEventListener('click', window.closeCreateModal);
        });

        // 헤더 "Create Schedule" CTA hijack
        document.querySelectorAll('[data-create-cta]').forEach(link => {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                window.openCreateModal();
            });
        });
    });
})();
