/**
 * owner-dashboard.js
 * Owner dashboard functionality
 *
 * Requires: window.OWNER_DATA to be defined before this script loads
 * - ownerId
 */

(function() {
    'use strict';

    const { ownerId } = window.OWNER_DATA;

    // ==================== Modal Functions ====================

    window.openCreateModal = function() {
        document.getElementById('createModal').classList.add('show');
    };

    window.closeCreateModal = function() {
        document.getElementById('createModal').classList.remove('show');
    };

    // ==================== Form Handlers ====================

    document.addEventListener('DOMContentLoaded', function() {
        // Create schedule form
        const createForm = document.getElementById('createScheduleForm');
        if (createForm) {
            createForm.addEventListener('submit', handleCreateSchedule);
        }

        // Copy link buttons
        document.querySelectorAll('.copy-link-btn').forEach(btn => {
            btn.addEventListener('click', function() {
                copyLink(this.dataset.owner, this.dataset.year, this.dataset.month);
            });
        });

        // Delete buttons
        document.querySelectorAll('.delete-btn').forEach(btn => {
            btn.addEventListener('click', function() {
                deleteSchedule(this.dataset.owner, this.dataset.year, this.dataset.month);
            });
        });
    });

    async function handleCreateSchedule(e) {
        e.preventDefault();
        const year = parseInt(document.getElementById('year').value);
        const month = parseInt(document.getElementById('month').value);

        try {
            await api.createSchedule(ownerId, year, month);
            window.location.reload();
        } catch (error) {
            toast.error(error.message);
        }
    }

    // ==================== Copy Link ====================

    function copyLink(ownerId, year, month) {
        const url = `${window.location.origin}/${ownerId}/${year}/${month}`;

        if (navigator.clipboard && window.isSecureContext) {
            navigator.clipboard.writeText(url)
                .then(() => alert('링크가 클립보드에 복사되었습니다!'))
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
            alert('링크가 클립보드에 복사되었습니다!');
        } catch (err) {
            prompt('아래 링크를 복사하세요:', text);
        }
        document.body.removeChild(textarea);
    }

    // ==================== Delete Schedule ====================

    async function deleteSchedule(ownerId, year, month) {
        if (!confirm('이 일정을 삭제하시겠습니까?')) {
            return;
        }
        try {
            await api.deleteSchedule(ownerId, year, month);
            window.location.reload();
        } catch (error) {
            toast.error(error.message);
        }
    }

})();
