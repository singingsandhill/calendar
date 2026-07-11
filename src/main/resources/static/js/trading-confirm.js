/**
 * trading-confirm.js
 * 위험 액션용 공통 ConfirmDialog 컴포넌트
 *
 * 사용법:
 *   TradingConfirm.show({
 *     title: '긴급 청산',
 *     body: '모든 포지션을 즉시 시장가로 청산합니다.',
 *     impact: 'KRW-ADA · 보유 12.4 → 즉시 매도',
 *     confirmText: 'CLOSE',
 *     countdownSec: 5,
 *     danger: true,
 *     onConfirm: async () => { await fetch(...); }
 *   })
 */
(function (root) {
    'use strict';

    let overlayEl = null;

    function ensureOverlay() {
        if (overlayEl) return overlayEl;
        overlayEl = document.createElement('div');
        overlayEl.id = 'trading-confirm-overlay';
        overlayEl.style.cssText = [
            'position:fixed', 'inset:0', 'background:rgba(0,0,0,0.6)',
            'display:none', 'align-items:center', 'justify-content:center',
            'z-index:9999', 'padding:1rem'
        ].join(';');
        document.body.appendChild(overlayEl);
        overlayEl.addEventListener('click', (e) => {
            if (e.target === overlayEl) close();
        });
        return overlayEl;
    }

    function close() {
        if (!overlayEl) return;
        overlayEl.style.display = 'none';
        overlayEl.innerHTML = '';
    }

    function escapeHtml(s) {
        if (s == null) return '';
        return String(s)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }

    function show(opts) {
        ensureOverlay();
        const {
            title = '확인',
            body = '',
            impact = '',
            confirmText = null,
            countdownSec = 0,
            danger = false,
            onConfirm = async () => {},
            confirmLabel = danger ? '실행' : '확인',
            cancelLabel = '취소'
        } = opts || {};

        const accent = danger ? '#dc2626' : '#2563eb';
        const accentHover = danger ? '#b91c1c' : '#1d4ed8';

        overlayEl.innerHTML = `
            <div role="dialog" aria-modal="true" aria-labelledby="tc-title"
                 style="background:#ffffff;color:#111827;max-width:440px;width:100%;
                        border-radius:0.75rem;box-shadow:0 25px 50px -12px rgba(0,0,0,0.5);
                        padding:1.25rem;">
                <div style="display:flex;align-items:center;gap:0.5rem;margin-bottom:0.5rem;">
                    ${danger ? '<span aria-hidden="true" style="font-size:1.25rem;color:'+accent+';">⚠</span>' : ''}
                    <h3 id="tc-title" style="font-size:1.125rem;font-weight:700;margin:0;">${escapeHtml(title)}</h3>
                </div>
                <p style="font-size:0.9rem;color:#374151;margin:0 0 0.75rem 0;line-height:1.45;">${escapeHtml(body)}</p>
                ${impact ? `<div style="background:#f3f4f6;border-left:3px solid ${accent};padding:0.5rem 0.75rem;border-radius:0.25rem;font-size:0.85rem;color:#111827;margin-bottom:0.75rem;">${escapeHtml(impact)}</div>` : ''}
                ${confirmText ? `
                    <label style="font-size:0.8rem;color:#374151;display:block;margin-bottom:0.25rem;">
                        확인 문자 <code style="background:#e5e7eb;padding:0 0.25rem;border-radius:0.25rem;">${escapeHtml(confirmText)}</code> 입력
                    </label>
                    <input id="tc-input" type="text" autocomplete="off" spellcheck="false"
                           style="width:100%;border:1px solid #d1d5db;border-radius:0.375rem;padding:0.5rem;font-size:0.9rem;margin-bottom:0.75rem;color:#111827;background:#ffffff;" />
                ` : ''}
                <div style="display:flex;align-items:center;gap:0.5rem;margin-bottom:0.75rem;">
                    <input id="tc-ack" type="checkbox" />
                    <label for="tc-ack" style="font-size:0.85rem;color:#374151;">위 영향을 확인했습니다.</label>
                </div>
                <div style="display:flex;justify-content:flex-end;gap:0.5rem;">
                    <button id="tc-cancel" type="button"
                            style="padding:0.5rem 1rem;border-radius:0.375rem;background:#e5e7eb;color:#111827;font-weight:500;border:none;cursor:pointer;">
                        ${escapeHtml(cancelLabel)}
                    </button>
                    <button id="tc-confirm" type="button" disabled
                            style="padding:0.5rem 1rem;border-radius:0.375rem;background:${accent};color:#ffffff;font-weight:600;border:none;cursor:not-allowed;opacity:0.5;">
                        <span id="tc-confirm-label">${escapeHtml(confirmLabel)}</span>
                    </button>
                </div>
                <div id="tc-error" role="alert" style="font-size:0.8rem;color:#dc2626;margin-top:0.5rem;min-height:1rem;"></div>
            </div>
        `;
        overlayEl.style.display = 'flex';

        const ackEl = overlayEl.querySelector('#tc-ack');
        const inputEl = overlayEl.querySelector('#tc-input');
        const confirmBtn = overlayEl.querySelector('#tc-confirm');
        const labelEl = overlayEl.querySelector('#tc-confirm-label');
        const cancelBtn = overlayEl.querySelector('#tc-cancel');
        const errorEl = overlayEl.querySelector('#tc-error');

        let countdownRemaining = countdownSec;
        const baseLabel = confirmLabel;

        function refreshState() {
            const ack = !!ackEl?.checked;
            const text = inputEl ? inputEl.value === confirmText : true;
            const cd = countdownRemaining <= 0;
            const ok = ack && text && cd;
            confirmBtn.disabled = !ok;
            confirmBtn.style.cursor = ok ? 'pointer' : 'not-allowed';
            confirmBtn.style.opacity = ok ? '1' : '0.5';
            confirmBtn.onmouseenter = () => { if (ok) confirmBtn.style.background = accentHover; };
            confirmBtn.onmouseleave = () => { confirmBtn.style.background = accent; };
            if (countdownRemaining > 0) {
                labelEl.textContent = `${baseLabel} (${countdownRemaining}s)`;
            } else {
                labelEl.textContent = baseLabel;
            }
        }

        if (ackEl) ackEl.addEventListener('change', refreshState);
        if (inputEl) inputEl.addEventListener('input', refreshState);
        cancelBtn.addEventListener('click', () => close());

        let timer = null;
        if (countdownSec > 0) {
            timer = setInterval(() => {
                countdownRemaining = Math.max(0, countdownRemaining - 1);
                refreshState();
                if (countdownRemaining <= 0) clearInterval(timer);
            }, 1000);
        }
        refreshState();

        confirmBtn.addEventListener('click', async () => {
            confirmBtn.disabled = true;
            labelEl.textContent = '실행 중...';
            errorEl.textContent = '';
            try {
                await onConfirm();
                close();
            } catch (e) {
                console.error(e);
                errorEl.textContent = '실패: ' + (e?.message || e);
                confirmBtn.disabled = false;
                labelEl.textContent = baseLabel;
            }
        });

        document.addEventListener('keydown', escHandler, { once: false });
        function escHandler(e) {
            if (e.key === 'Escape') {
                close();
                document.removeEventListener('keydown', escHandler);
                if (timer) clearInterval(timer);
            }
        }
    }

    root.TradingConfirm = { show, close };
})(window);
