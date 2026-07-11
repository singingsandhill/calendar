/**
 * trading-globals.js
 * 전 trading 페이지 공통 글로벌 상태바, fetch 유틸, 토스트
 *
 * 노출:
 *   TradingFetch.json(url, opts) - AbortController + 백오프
 *   TradingToast.push(level, message) - level: ok|warning|critical
 *   TradingFreshness.format(seconds) / .since(date)
 */
(function (root) {
    'use strict';

    // ==================== Toast ====================
    const Toast = {
        push(level, message, ttlMs = 5000) {
            const container = document.getElementById('trading-toast-container');
            if (!container) return;
            const el = document.createElement('div');
            el.className = 'toast toast-' + (level || 'ok');
            el.textContent = message;
            container.appendChild(el);
            setTimeout(() => el.remove(), ttlMs);
        }
    };

    // ==================== Freshness ====================
    const Freshness = {
        format(seconds) {
            if (seconds == null || isNaN(seconds)) return '-';
            const s = Math.max(0, Math.round(seconds));
            if (s < 60) return s + 's ago';
            if (s < 3600) return Math.floor(s / 60) + 'm ago';
            return Math.floor(s / 3600) + 'h ago';
        },
        since(dateLike) {
            if (!dateLike) return null;
            const d = new Date(dateLike);
            if (isNaN(d.getTime())) return null;
            return (Date.now() - d.getTime()) / 1000;
        }
    };

    // ==================== Fetch with AbortController + backoff ====================
    const Fetch = {
        async json(url, opts = {}) {
            const ctrl = new AbortController();
            const timeoutMs = opts.timeoutMs || 8000;
            const timer = setTimeout(() => ctrl.abort(), timeoutMs);
            try {
                const res = await fetch(url, {
                    ...opts,
                    signal: ctrl.signal,
                    headers: { 'Accept': 'application/json', ...(opts.headers || {}) }
                });
                if (!res.ok) {
                    throw new Error('HTTP ' + res.status);
                }
                return await res.json();
            } finally {
                clearTimeout(timer);
            }
        }
    };

    // ==================== Global Status Bar ====================
    const StatusBar = {
        botFailCount: 0,
        exchangeFailCount: 0,
        lastBotData: null,
        lastVerifyOkAt: null,

        setDot(id, level) {
            const el = document.getElementById(id);
            if (!el) return;
            el.className = 'status-dot ' + ({
                ok: 'status-dot-ok',
                warn: 'status-dot-warn',
                crit: 'status-dot-crit'
            }[level] || 'status-dot-unknown');
        },

        setText(id, text) {
            const el = document.getElementById(id);
            if (el) el.textContent = text;
        },

        async pollBot() {
            try {
                const data = await Fetch.json('/api/trading/bot/status');
                this.lastBotData = data;
                this.botFailCount = 0;

                let level, label;
                const loopAge = Freshness.since(data.lastLoopAt);
                if (!data.running) {
                    level = 'warn'; label = 'STOPPED';
                } else if (data.paused) {
                    level = 'warn'; label = 'PAUSED';
                } else if (loopAge != null && loopAge > 120) {
                    level = 'crit'; label = 'STALLED';
                } else {
                    level = 'ok'; label = 'RUNNING';
                }

                this.setDot('gs-bot-dot', level);
                this.setText('gs-bot-text', label);
                this.setText('gs-bot-loop',
                    loopAge != null ? '(loop ' + Freshness.format(loopAge) + ')' : '');

                // 동기화: 메인 인디케이터(상단 우측)도 갱신
                const mainDot = document.getElementById('status-dot');
                const mainText = document.getElementById('status-text');
                if (mainDot && mainText) {
                    mainDot.className = 'status-dot ' + ({ ok:'status-dot-ok', warn:'status-dot-warn', crit:'status-dot-crit' }[level] || 'status-dot-unknown');
                    mainText.textContent = data.running ? (data.paused ? 'Paused' : 'Running') : 'Stopped';
                }

                if (data.lastError) {
                    this.pushAlert('warning', '봇 오류: ' + data.lastError);
                }
            } catch (e) {
                this.botFailCount++;
                this.setDot('gs-bot-dot', 'crit');
                this.setText('gs-bot-text', 'OFFLINE');
                this.setText('gs-bot-loop', '');
            }
            this.setText('gs-updated', new Date().toLocaleTimeString('ko-KR'));
        },

        async pollFreshness() {
            // 데이터 신선도: profit/summary로 가격 응답성 확인
            try {
                await Fetch.json('/api/trading/profit/summary');
                this.exchangeFailCount = 0;
                this.lastVerifyOkAt = new Date();
                this.setDot('gs-exchange-dot', 'ok');
                this.setText('gs-exchange-text', 'OK');
                this.setDot('gs-data-dot', 'ok');
                this.setText('gs-data-text', 'OK');
            } catch (e) {
                this.exchangeFailCount++;
                if (this.exchangeFailCount >= 2) {
                    this.setDot('gs-exchange-dot', 'crit');
                    this.setText('gs-exchange-text', 'FAIL');
                    this.setDot('gs-data-dot', 'warn');
                    this.setText('gs-data-text', 'STALE');
                } else {
                    this.setDot('gs-exchange-dot', 'warn');
                    this.setText('gs-exchange-text', 'RETRY');
                }
            }
        },

        alerts: [],
        pushAlert(level, message) {
            const key = level + ':' + message;
            if (this.alerts.find(a => a.key === key)) return;
            this.alerts.push({ key, level, message, at: Date.now() });
            // 최근 5개만 유지
            if (this.alerts.length > 5) this.alerts.shift();
            this.renderAlerts();
            if (level === 'critical') Toast.push('critical', message);
        },
        renderAlerts() {
            const el = document.getElementById('gs-alerts');
            if (!el) return;
            const count = this.alerts.length;
            if (count === 0) {
                el.innerHTML = '<span class="text-gray-400">알림 없음</span>';
                return;
            }
            const worst = this.alerts.some(a => a.level === 'critical') ? 'critical'
                       : this.alerts.some(a => a.level === 'warning') ? 'warning' : 'notice';
            el.innerHTML = '<span class="badge badge-' + worst + '" title="' +
                this.alerts.map(a => a.message.replace(/"/g, '&quot;')).join(' / ') +
                '">⚠ 알림 ' + count + '건</span>';
        },

        start() {
            this.pollBot();
            this.pollFreshness();
            setInterval(() => this.pollBot(), 5000);
            setInterval(() => this.pollFreshness(), 15000);
        }
    };

    document.addEventListener('DOMContentLoaded', () => StatusBar.start());

    root.TradingFetch = Fetch;
    root.TradingToast = Toast;
    root.TradingFreshness = Freshness;
    root.TradingStatusBar = StatusBar;
})(window);
