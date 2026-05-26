// DateDate analytics helpers.
// 기존 6개 dataLayer 이벤트는 인라인 push 유지 (P2-1 마이그레이션 범위).
// 신규 이벤트가 필요로 하는 PII 해시·owner 마커만 여기 정의.
// classic script (non-module) — 인라인/defer 양 환경에서 동일하게 window.DDAnalytics 로 접근.

(function (global) {
    var OWNED_SCHEDULES_KEY = 'dd_owned_schedules';

    async function sha256Hex(input) {
        if (!input || !global.crypto || !global.crypto.subtle) return null;
        try {
            var buf = new TextEncoder().encode(String(input));
            var hash = await global.crypto.subtle.digest('SHA-256', buf);
            return Array.from(new Uint8Array(hash))
                .map(function (b) { return b.toString(16).padStart(2, '0'); }).join('');
        } catch (_) {
            return null;
        }
    }

    function markOwnedSchedule(ownerId) {
        if (!ownerId) return;
        try {
            var raw = localStorage.getItem(OWNED_SCHEDULES_KEY);
            var arr = raw ? JSON.parse(raw) : [];
            if (!Array.isArray(arr)) return;
            if (arr.indexOf(ownerId) === -1) {
                arr.push(ownerId);
                localStorage.setItem(OWNED_SCHEDULES_KEY, JSON.stringify(arr));
            }
        } catch (_) { /* localStorage 비활성 / quota 초과 → 무시 */ }
    }

    function isOwnedSchedule(ownerId) {
        if (!ownerId) return false;
        try {
            var raw = localStorage.getItem(OWNED_SCHEDULES_KEY);
            if (!raw) return false;
            var arr = JSON.parse(raw);
            return Array.isArray(arr) && arr.indexOf(ownerId) !== -1;
        } catch (_) {
            return false;
        }
    }

    global.DDAnalytics = {
        sha256Hex: sha256Hex,
        markOwnedSchedule: markOwnedSchedule,
        isOwnedSchedule: isOwnedSchedule
    };
})(typeof window !== 'undefined' ? window : globalThis);
