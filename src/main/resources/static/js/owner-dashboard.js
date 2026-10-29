/**
 * owner-dashboard.js
 * Owner dashboard 카드 액션 (복사 / 삭제).
 *
 * 모달 열기·생성 폼 처리는 create-schedule-modal.js 가 담당.
 * Requires: window.OWNER_DATA = { ownerId, messages } 가 사전 정의되어야 함 (문구는 메시지 키 — dashboard.html).
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
                .then(() => alert(window.OWNER_DATA.messages.linkCopied))
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
            alert(window.OWNER_DATA.messages.linkCopied);
        } catch (err) {
            prompt(window.OWNER_DATA.messages.linkCopyPrompt, text);
        }
        document.body.removeChild(textarea);
    }

    async function deleteSchedule(ownerId, year, month) {
        if (!confirm(window.OWNER_DATA.messages.deleteConfirm)) {
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
