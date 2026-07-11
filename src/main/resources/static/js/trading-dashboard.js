/**
 * trading-dashboard.js
 * Operator-focused dashboard for the trading bot.
 *
 * Requires: LightweightCharts, trading-globals.js, trading-confirm.js
 */
(function () {
    'use strict';

    let chart, candleSeries;
    let lastTickerData = null;
    let lastSummaryData = null;
    let stopLossPct = 0.03;     // 기본값. /api/trading/today 또는 settings에서 보정 가능
    let takeProfitPct = 0.15;

    const INTERVAL_INDICATORS = 10000;
    const INTERVAL_SUMMARY = 15000;
    const INTERVAL_POSITIONS = 15000;
    const INTERVAL_TRADE_MARKERS = 30000;
    const INTERVAL_EVENTS = 20000;

    let lastEventsAt = null;

    document.addEventListener('DOMContentLoaded', () => {
        initChart();
        loadIndicators();
        loadSummary();
        loadActivePositions();
        loadClosedPositions();
        loadEvents();

        setInterval(loadIndicators, INTERVAL_INDICATORS);
        setInterval(loadSummary, INTERVAL_SUMMARY);
        setInterval(loadActivePositions, INTERVAL_POSITIONS);
        setInterval(loadClosedPositions, INTERVAL_POSITIONS * 2);
        setInterval(loadTradeMarkers, INTERVAL_TRADE_MARKERS);
        setInterval(loadEvents, INTERVAL_EVENTS);
        setInterval(updateEventsFreshness, 1000);

        document.getElementById('btn-toggle-bot')?.addEventListener('click', toggleBot);
        document.getElementById('btn-emergency')?.addEventListener('click', emergencyClose);
        document.getElementById('events-min-level')?.addEventListener('change', loadEvents);
    });

    window.addEventListener('resize', () => {
        if (chart) {
            const w = document.getElementById('chart-container')?.clientWidth;
            if (w) chart.applyOptions({ width: w });
        }
    });

    // ==================== Chart ====================
    function initChart() {
        const container = document.getElementById('chart-container');
        if (!container || typeof LightweightCharts === 'undefined') return;

        chart = LightweightCharts.createChart(container, {
            layout: { background: { type: 'solid', color: '#1f2937' }, textColor: '#d1d5db' },
            grid: { vertLines: { color: '#374151' }, horzLines: { color: '#374151' } },
            crosshair: { mode: LightweightCharts.CrosshairMode.Normal },
            rightPriceScale: { borderColor: '#374151' },
            timeScale: { borderColor: '#374151', timeVisible: true, secondsVisible: false }
        });

        candleSeries = chart.addCandlestickSeries({
            upColor: '#22c55e', downColor: '#ef4444',
            borderDownColor: '#ef4444', borderUpColor: '#22c55e',
            wickDownColor: '#ef4444', wickUpColor: '#22c55e'
        });

        loadChartData();
    }

    async function loadChartData() {
        try {
            const data = await TradingFetch.json('/api/trading/candles?count=200');
            if (data.candles && data.candles.length > 0) {
                const candleData = data.candles.map(c => ({
                    time: new Date(c.time).getTime() / 1000,
                    open: c.open, high: c.high, low: c.low, close: c.close
                })).reverse();
                candleSeries.setData(candleData);
                const last = candleData[candleData.length - 1];
                document.getElementById('current-price').textContent =
                    last.close.toLocaleString() + ' KRW';
                await loadTradeMarkers();
            }
        } catch (e) { console.error('chart load fail', e); }
    }

    async function loadTradeMarkers() {
        try {
            const trades = await TradingFetch.json('/api/trading/chart/trades?minutes=200');
            if (trades && trades.length > 0 && candleSeries) {
                const markers = trades.map(t => ({
                    time: new Date(t.time).getTime() / 1000,
                    position: t.type === 'BUY' ? 'belowBar' : 'aboveBar',
                    color: t.type === 'BUY' ? '#22c55e' : '#ef4444',
                    shape: t.type === 'BUY' ? 'arrowUp' : 'arrowDown',
                    text: t.type + ' ' + t.price.toLocaleString()
                }));
                candleSeries.setMarkers(markers);
            }
        } catch (e) { /* markers optional */ }
    }

    // ==================== Indicators ====================
    async function loadIndicators() {
        try {
            const data = await TradingFetch.json('/api/trading/ticker');
            lastTickerData = data;
            setText('indicator-rsi', data.rsi != null ? data.rsi.toFixed(2) : '-');
            setText('indicator-stoch',
                (data.stochK != null && data.stochD != null)
                    ? data.stochK.toFixed(1) + ' / ' + data.stochD.toFixed(1) : '-');
            setText('indicator-ma5', data.ma5 != null ? data.ma5.toLocaleString() : '-');
            setText('indicator-ma20', data.ma20 != null ? data.ma20.toLocaleString() : '-');
            setText('indicator-ma60', data.ma60 != null ? data.ma60.toLocaleString() : '-');

            if (data.currentPrice != null && data.ma60 != null) {
                const regimeEl = document.getElementById('indicator-regime');
                if (data.currentPrice > data.ma60) {
                    regimeEl.textContent = 'BULL (>MA60)';
                    regimeEl.className = 'font-medium positive';
                } else if (data.currentPrice < data.ma60) {
                    regimeEl.textContent = 'BEAR (<MA60)';
                    regimeEl.className = 'font-medium negative';
                } else {
                    regimeEl.textContent = 'NEUTRAL';
                    regimeEl.className = 'font-medium';
                }
            }

            if (data.currentPrice != null) {
                document.getElementById('current-price').textContent =
                    data.currentPrice.toLocaleString() + ' KRW';
            }
        } catch (e) { /* keep last good */ }
    }

    // ==================== Summary cards ====================
    async function loadSummary() {
        try {
            const [summary, today, botStatus] = await Promise.all([
                TradingFetch.json('/api/trading/profit/summary'),
                TradingFetch.json('/api/trading/today').catch(() => null),
                TradingFetch.json('/api/trading/bot/status').catch(() => null)
            ]);
            lastSummaryData = summary;

            // Total
            setText('card-total-value', formatKRW(summary.totalValue));
            setText('card-total-sub',
                'KRW ' + formatKRW(summary.krwBalance) + ' · ' +
                (summary.coinBalance ? summary.coinBalance.toFixed(4) : '-') + ' coin');

            // Today P&L
            if (today) {
                const cls = today.realizedPnl >= 0 ? 'positive' : 'negative';
                document.getElementById('card-today-pnl').innerHTML =
                    `<span class="${cls}">${signed(today.realizedPnl)} KRW</span>`;
                setText('card-today-sub',
                    `${today.doneTrades} done · ${today.failedTrades} fail · ${today.openPositions} open`);
            } else {
                setText('card-today-pnl', '-');
                setText('card-today-sub', '');
            }

            // Unrealized
            const upClass = summary.unrealizedPnl >= 0 ? 'positive' : 'negative';
            document.getElementById('card-unrealized').innerHTML =
                `<span class="${upClass}">${summary.unrealizedPnlPct.toFixed(2)}%</span>`;
            setText('card-unrealized-sub', signed(summary.unrealizedPnl) + ' KRW');

            // Allocation (Cash / Coin)
            const coinPct = (summary.coinRatio || 0) * 100;
            const cashPct = 100 - coinPct;
            setText('card-allocation', `${cashPct.toFixed(0)}% / ${coinPct.toFixed(0)}%`);
            const gauge = document.getElementById('card-allocation-gauge');
            if (gauge) gauge.style.width = coinPct.toFixed(1) + '%';
            // 리밸런싱 상태 호출해 편차 표시
            try {
                const reb = await TradingFetch.json('/api/trading/rebalance/status');
                if (reb && reb.targetRatio != null && reb.deviation != null) {
                    const targetPct = (reb.targetRatio * 100).toFixed(0);
                    const devPct = (reb.deviation * 100).toFixed(1);
                    const need = Math.abs(reb.deviation) >= reb.deviationTrigger;
                    const sub = document.getElementById('card-allocation-sub');
                    sub.innerHTML = `목표 ${targetPct}% · 편차 ${devPct}% ` +
                        (need ? '<span class="badge badge-warning">조정 필요</span>'
                              : '<span class="badge badge-ok">밸런스</span>');
                    if (need && reb.cooldownRemainingSec === 0) {
                        TradingStatusBar.pushAlert('warning',
                            `리밸런싱 편차 ${devPct}% (목표 ${targetPct}%)`);
                    }
                }
            } catch (e) { /* optional */ }

            // Win Rate
            setText('card-winrate', summary.winRate.toFixed(1) + '%');
            setText('card-winrate-sub', `${summary.totalTrades} trades · avg ${summary.avgPnlPct.toFixed(2)}%`);

            // Bot card
            if (botStatus) {
                renderBotCard(botStatus);
            }
        } catch (e) { console.error('summary fail', e); }
    }

    function renderBotCard(s) {
        const dot = document.getElementById('card-bot-dot');
        const text = document.getElementById('card-bot-text');
        const sub = document.getElementById('card-bot-sub');
        const card = document.getElementById('card-bot');
        const btn = document.getElementById('btn-toggle-bot');

        const loopAge = TradingFreshness.since(s.lastLoopAt);
        let level, label;
        if (!s.running) { level='warn'; label='STOPPED'; }
        else if (s.paused) { level='warn'; label='PAUSED'; }
        else if (loopAge != null && loopAge > 120) { level='crit'; label='STALLED'; }
        else { level='ok'; label='RUNNING'; }

        dot.className = 'status-dot ' + ({ ok:'status-dot-ok', warn:'status-dot-warn', crit:'status-dot-crit' }[level]);
        text.textContent = label;
        card.className = 'card ' + ({ ok:'card-ok', warn:'card-warning', crit:'card-critical' }[level]);
        sub.textContent = (loopAge != null ? 'loop ' + TradingFreshness.format(loopAge) : '-') +
                          (s.lastError ? ' · err: ' + s.lastError.substring(0, 40) : '');
        btn.textContent = s.running ? 'Stop' : 'Start';
    }

    // ==================== Active Positions ====================
    async function loadActivePositions() {
        try {
            const positions = await TradingFetch.json('/api/trading/positions?size=20');
            const open = positions.filter(p => p.status === 'OPEN');
            const meta = document.getElementById('active-positions-meta');
            meta.textContent = open.length + ' open';

            const container = document.getElementById('active-positions');
            if (open.length === 0) {
                container.innerHTML = '<div class="text-sm text-gray-500">현재 활성 포지션이 없습니다.</div>';
                return;
            }

            const currentPrice = lastTickerData?.currentPrice || lastSummaryData?.currentPrice;
            container.innerHTML = open.map(p => renderActivePosition(p, currentPrice)).join('');
        } catch (e) { console.error('active positions fail', e); }
    }

    function renderActivePosition(p, currentPrice) {
        const entry = p.entryPrice;
        const stop = entry * (1 - stopLossPct);
        const tp = entry * (1 + takeProfitPct);
        const now = currentPrice || entry;
        const pnlPct = ((now - entry) / entry) * 100;
        const pnlAbs = (now - entry) * (p.entryVolume || 0);

        // gauge: 손절(0%) → 진입(20%) → 현재가 → 익절(100%)
        const range = tp - stop;
        const ratio = Math.max(0, Math.min(1, (now - stop) / range));
        const entryRatio = Math.max(0, Math.min(1, (entry - stop) / range));

        // 가까운 트리거까지 거리
        const distToStop = ((now - stop) / now) * 100;
        const distToTp = ((tp - now) / now) * 100;
        const nearestLabel = Math.abs(distToStop) < Math.abs(distToTp)
            ? `손절까지 ${distToStop.toFixed(2)}%`
            : `익절까지 ${distToTp.toFixed(2)}%`;

        let level = 'card-ok';
        if (distToStop < 1.0) level = 'card-critical';
        else if (distToStop < 2.0 || pnlPct < -2.0) level = 'card-warning';
        else if (pnlPct >= 10) level = 'card-notice';

        const pnlCls = pnlPct >= 0 ? 'positive' : 'negative';
        const opened = p.openedAt ? new Date(p.openedAt) : null;
        const heldMin = opened ? Math.floor((Date.now() - opened.getTime()) / 60000) : null;

        return `
        <div class="card ${level}">
            <div class="flex items-center justify-between mb-2 flex-wrap">
                <div class="flex items-center gap-2">
                    <span class="font-semibold dark:text-white">${escapeHtml(p.market)}</span>
                    <span class="badge badge-ok">OPEN</span>
                    ${heldMin != null ? `<span class="text-xs text-gray-500">${heldMin}분 보유</span>` : ''}
                </div>
                <div class="text-sm">
                    <span class="text-gray-500">entry</span>
                    <span class="font-medium dark:text-white">${entry.toLocaleString()}</span>
                    <span class="text-gray-400">→</span>
                    <span class="font-medium dark:text-white">${now.toLocaleString()}</span>
                    <span class="${pnlCls} font-semibold ml-2">${signed(pnlPct.toFixed(2))}%</span>
                    <span class="${pnlCls} text-xs ml-1">(${signed(Math.round(pnlAbs))} KRW)</span>
                </div>
            </div>
            <div class="relative" style="margin: 8px 0;">
                <div class="gauge" style="height:8px;">
                    <div class="gauge-fill" style="width:${(ratio*100).toFixed(1)}%; background:${pnlPct>=0?'#22c55e':'#ef4444'};"></div>
                </div>
                <div class="gauge-marker" style="left:${(entryRatio*100).toFixed(1)}%;"></div>
            </div>
            <div class="flex items-center justify-between text-xs text-gray-500">
                <span>손절 ${Math.round(stop).toLocaleString()} (-${(stopLossPct*100).toFixed(0)}%)</span>
                <span class="font-medium">${nearestLabel}</span>
                <span>익절 ${Math.round(tp).toLocaleString()} (+${(takeProfitPct*100).toFixed(0)}%)</span>
            </div>
            <div class="mt-3 flex gap-2">
                <button type="button" class="btn-danger text-xs" data-action="manual-sell" data-volume="${p.entryVolume || 0}">수동 청산</button>
            </div>
        </div>`;
    }

    // 위임: 수동 청산 버튼
    document.addEventListener('click', (e) => {
        const t = e.target;
        if (t?.dataset?.action === 'manual-sell') {
            const volume = Number(t.dataset.volume || 0);
            if (!volume) return;
            TradingConfirm.show({
                title: '수동 청산',
                body: `해당 포지션 (volume ${volume.toFixed(4)})을 시장가로 즉시 매도합니다.`,
                impact: '시장가 슬리피지·수수료 발생. 자동매매 봇은 계속 동작합니다.',
                confirmText: 'SELL',
                countdownSec: 3,
                danger: true,
                onConfirm: async () => {
                    const r = await fetch('/api/trading/bot/manual/sell', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ volume })
                    });
                    const data = await r.json();
                    TradingToast.push(data.success ? 'ok' : 'critical',
                        data.message || (data.success ? 'Sell ordered' : 'Sell failed'));
                    setTimeout(() => { loadActivePositions(); loadSummary(); }, 800);
                }
            });
        }
    });

    // ==================== Closed positions table ====================
    async function loadClosedPositions() {
        try {
            const positions = await TradingFetch.json('/api/trading/positions?size=10');
            const closed = positions.filter(p => p.status !== 'OPEN').slice(0, 10);
            const tbody = document.getElementById('positions-table');
            if (closed.length === 0) {
                tbody.innerHTML = '<tr><td colspan="5" class="text-center py-4 text-gray-500">종료된 포지션 없음</td></tr>';
                return;
            }
            tbody.innerHTML = closed.map(p => `
                <tr class="border-t border-gray-700">
                    <td class="py-2">${p.closedAt ? formatDateTime(p.closedAt) : '-'}</td>
                    <td class="py-2 text-right">${p.entryPrice ? p.entryPrice.toLocaleString() : '-'}</td>
                    <td class="py-2 text-right">${p.exitPrice ? p.exitPrice.toLocaleString() : '-'}</td>
                    <td class="py-2 text-right ${(p.realizedPnlPct || 0) >= 0 ? 'positive' : 'negative'}">
                        ${p.realizedPnlPct != null ? signed(p.realizedPnlPct.toFixed(2)) + '%' : '-'}
                    </td>
                    <td class="py-2 pl-2">${reasonBadge(p.closeReason)}</td>
                </tr>
            `).join('');
        } catch (e) { console.error('closed positions fail', e); }
    }

    function reasonBadge(reason) {
        if (!reason) return '<span class="badge badge-neutral">-</span>';
        const map = {
            STOP_LOSS: 'badge-critical', TAKE_PROFIT: 'badge-ok',
            TRAILING_STOP: 'badge-notice', SIGNAL: 'badge-notice',
            REBALANCE: 'badge-neutral', MANUAL: 'badge-warning',
            EMERGENCY: 'badge-critical'
        };
        return `<span class="badge ${map[reason] || 'badge-neutral'}">${escapeHtml(reason)}</span>`;
    }

    // ==================== Bot control ====================
    async function toggleBot() {
        const status = await TradingFetch.json('/api/trading/bot/status');
        const running = !!status.running;
        const action = running ? 'stop' : 'start';
        TradingConfirm.show({
            title: running ? '봇 중지' : '봇 시작',
            body: running
                ? '자동매매 루프를 중지합니다. 활성 포지션은 유지됩니다.'
                : '자동매매 루프를 시작합니다. 신호 점수에 따라 매수/매도가 자동으로 발생합니다.',
            impact: running ? '신호 평가 중단 / 리스크 감시 중단' : '매분 +5초 트레이딩 루프 활성화',
            danger: running,
            confirmText: null,
            countdownSec: running ? 2 : 0,
            confirmLabel: running ? 'Stop Bot' : 'Start Bot',
            onConfirm: async () => {
                const r = await fetch(`/api/trading/bot/${action}`, { method: 'POST' });
                const data = await r.json();
                TradingToast.push(data.success ? 'ok' : 'warning', data.message);
                setTimeout(() => { loadSummary(); TradingStatusBar.pollBot(); }, 600);
            }
        });
    }

    async function emergencyClose() {
        TradingConfirm.show({
            title: '긴급 청산',
            body: '모든 활성 포지션을 즉시 시장가로 청산합니다.',
            impact: '슬리피지/수수료 발생. 봇은 청산 후에도 가동 상태를 유지합니다.',
            confirmText: 'EMERGENCY',
            countdownSec: 5,
            danger: true,
            confirmLabel: 'Execute Close',
            onConfirm: async () => {
                const r = await fetch('/api/trading/bot/emergency-close', { method: 'POST' });
                const data = await r.json();
                TradingToast.push(data.success ? 'ok' : 'critical', data.message);
                setTimeout(() => { loadActivePositions(); loadSummary(); }, 800);
            }
        });
    }

    // ==================== Recent Events ====================
    async function loadEvents() {
        const select = document.getElementById('events-min-level');
        const minLevel = select ? select.value : '';
        const params = new URLSearchParams({ limit: '20' });
        if (minLevel) params.set('minLevel', minLevel);
        try {
            const events = await TradingFetch.json('/api/trading/events?' + params.toString());
            const list = document.getElementById('events-list');
            if (!list) return;
            if (!events || events.length === 0) {
                list.innerHTML = '<div class="text-gray-500">기록된 이벤트 없음</div>';
            } else {
                list.innerHTML = events.map(renderEventRow).join('');
            }
            lastEventsAt = Date.now();
            updateEventsFreshness();
        } catch (e) {
            const list = document.getElementById('events-list');
            if (list) list.innerHTML = `<div class="negative">이벤트 로드 실패: ${escapeHtml(e.message)}</div>`;
        }
    }

    function renderEventRow(ev) {
        const badgeClass = ({
            OK: 'badge-ok', NOTICE: 'badge-notice',
            WARNING: 'badge-warning', CRITICAL: 'badge-critical'
        }[ev.level] || 'badge-neutral');
        const icon = ({
            OK: '✓', NOTICE: 'ⓘ', WARNING: '⚠', CRITICAL: '⛔'
        }[ev.level] || '·');
        const ts = ev.createdAt ? new Date(ev.createdAt) : null;
        const tsLabel = ts ? ts.toLocaleTimeString('ko-KR') : '-';
        return `
            <div class="flex items-start gap-2 border-b border-gray-100 dark:border-gray-800 py-1">
                <span class="text-xs text-gray-400 w-16 flex-shrink-0 mt-0.5">${escapeHtml(tsLabel)}</span>
                <span class="badge ${badgeClass} flex-shrink-0">${icon} ${escapeHtml(ev.level)}</span>
                <span class="text-xs text-gray-500 flex-shrink-0 mt-0.5">${escapeHtml(ev.eventType)}</span>
                <span class="flex-1 dark:text-gray-200">${escapeHtml(ev.message)}</span>
            </div>
        `;
    }

    function updateEventsFreshness() {
        const el = document.getElementById('events-updated');
        if (!el) return;
        if (!lastEventsAt) { el.textContent = '-'; return; }
        const seconds = Math.floor((Date.now() - lastEventsAt) / 1000);
        el.textContent = (window.TradingFreshness ? TradingFreshness.format(seconds) : seconds + 's ago');
    }

    // ==================== Helpers ====================
    function setText(id, t) {
        const el = document.getElementById(id);
        if (el) el.textContent = t;
    }
    function formatKRW(v) {
        if (v == null) return '-';
        return Math.round(v).toLocaleString();
    }
    function signed(v) {
        const n = typeof v === 'number' ? v : Number(v);
        if (isNaN(n)) return v;
        return (n >= 0 ? '+' : '') + n.toLocaleString();
    }
    function formatDateTime(s) {
        const d = new Date(s);
        return d.toLocaleDateString('ko-KR') + ' ' +
               d.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' });
    }
    function escapeHtml(s) {
        if (s == null) return '';
        return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;')
            .replace(/"/g,'&quot;').replace(/'/g,'&#39;');
    }
})();
