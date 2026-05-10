/**
 * owner-dashboard.js
 * Owner dashboard 카드 액션 (복사 / 삭제).
 *
 * 모달 열기·생성 폼 처리는 create-schedule-modal.js 가 담당.
 * Requires: window.OWNER_DATA = { ownerId } 가 사전 정의되어야 함.
 */

(function() {
    'use strict';

    document.addEventListener('DOMContentLoaded', function() {
        document.querySelectorAll('.copy-link-btn').forEach(btn => {
            btn.addEventListener('click', function() {
                copyLink(this.dataset.owner, this.dataset.year, this.dataset.month);
            });
        });

        document.querySelectorAll('.delete-btn').forEach(btn => {
            btn.addEventListener('click', function() {
                deleteSchedule(this.dataset.owner, this.dataset.year, this.dataset.month);
            });
        });
    });

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
