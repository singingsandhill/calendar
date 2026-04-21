/**
 * trading-trades.js
 * Trades 페이지: 30s 폴링, 필터 칩, 실패 강조, 종료 포지션 사유 배지
 */
(function () {
    'use strict';

    const PAGE_SIZE = 20;
    const POLL_MS = 30000;

    let currentPage = 0;
    let currentFilter = 'ALL';
    let dailyChart;
    let lastUpdatedAt = null;
    let pollTimer = null;

    document.addEventListener('DOMContentLoaded', () => {
        bindFilters();
        bindPager();
        document.getElementById('trades-refresh-btn')?.addEventListener('click', () => loadTrades(currentPage));

        loadProfitSummary();
        loadTrades(0);
        loadPositions();
        loadDailyChart();

        pollTimer = setInterval(() => {
            loadProfitSummary();
            loadTrades(currentPage);
            loadPositions();
        }, POLL_MS);

        // 사용자가 페이지 벗어나면 폴링 정지
        document.addEventListener('visibilitychange', () => {
            if (document.hidden) {
                clearInterval(pollTimer);
                pollTimer = null;
            } else if (!pollTimer) {
                pollTimer = setInterval(() => {
                    loadProfitSummary();
                    loadTrades(currentPage);
                    loadPositions();
                }, POLL_MS);
            }
        });

        // freshness ticker
        setInterval(updateFreshnessLabel, 1000);
    });

    function bindFilters() {
        const chips = document.querySelectorAll('#trades-filter-chips button');
        chips.forEach(c => {
            c.addEventListener('click', () => {
                currentFilter = c.dataset.filter;
                chips.forEach(x => {
                    const active = x === c;
                    x.className = 'badge ' + (active ? 'badge-notice' : 'badge-neutral');
                    x.setAttribute('aria-selected', active ? 'true' : 'false');
                });
                currentPage = 0;
                loadTrades(0);
            });
        });
    }

    function bindPager() {
        document.getElementById('prev-btn')?.addEventListener('click', () => {
            if (currentPage > 0) loadTrades(currentPage - 1);
        });
        document.getElementById('next-btn')?.addEventListener('click', () => {
            loadTrades(currentPage + 1);
        });
    }

    function updateFreshnessLabel() {
        const el = document.getElementById('trades-updated');
        if (!el) return;
        if (!lastUpdatedAt) { el.textContent = '-'; return; }
        const seconds = Math.floor((Date.now() - lastUpdatedAt) / 1000);
        el.textContent = (window.TradingFreshness ? TradingFreshness.format(seconds) : seconds + 's ago');
    }

    async function loadProfitSummary() {
        try {
            const data = await TradingFetch.json('/api/trading/profit/summary');
            const pnlClass = data.realizedPnl >= 0 ? 'positive' : 'negative';
            document.getElementById('realized-pnl').innerHTML =
                `<span class="${pnlClass}">${formatKRW(data.realizedPnl)} KRW</span>`;
            document.getElementById('total-trades').textContent = data.totalTrades;
            document.getElementById('win-rate').textContent = data.winRate.toFixed(1) + '%';
            document.getElementById('avg-pnl').innerHTML =
                `<span class="${data.avgPnlPct >= 0 ? 'positive' : 'negative'}">${signed(data.avgPnlPct.toFixed(2))}%</span>`;
            document.getElementById('total-fees').innerHTML =
                `<span class="negative">-${formatKRW(Math.abs(data.totalFeesPaid || 0))} KRW</span>`;
        } catch (e) { console.error('summary fail', e); }
    }

    async function loadTrades(page) {
        if (page < 0) page = 0;

        try {
            // 필터된 결과 충분한 사이즈를 보장하기 위해 page-size 다소 크게 요청 후 클라이언트 필터
            const fetchSize = currentFilter === 'ALL' ? PAGE_SIZE : 100;
            const trades = await TradingFetch.json(
                `/api/trading/trades?page=${page}&size=${fetchSize}`);

            const filtered = applyFilter(trades);
            const tbody = document.getElementById('trades-table');

            if (filtered.length === 0) {
                tbody.innerHTML =
                    `<tr><td colspan="6" class="text-center py-8 text-gray-500">조건에 맞는 거래 없음</td></tr>`;
            } else {
                tbody.innerHTML = filtered.slice(0, PAGE_SIZE).map(renderTradeRow).join('');
            }

            currentPage = page;
            document.getElementById('page-info').textContent = `Page ${page + 1}`;
            document.getElementById('prev-btn').disabled = page === 0;
            // 다음 페이지 비활성: 원본(filter 미적용) 결과가 페이지 사이즈보다 작으면 끝
            document.getElementById('next-btn').disabled = trades.length < fetchSize;

            lastUpdatedAt = Date.now();
            updateFreshnessLabel();
        } catch (e) {
            console.error('trades fail', e);
            const tbody = document.getElementById('trades-table');
            tbody.innerHTML = `<tr><td colspan="6" class="text-center py-8 negative">로드 실패: ${escapeHtml(e.message)}</td></tr>`;
        }
    }

    function applyFilter(trades) {
        switch (currentFilter) {
            case 'DONE': return trades.filter(t => t.status === 'DONE');
            case 'WAIT': return trades.filter(t => t.status === 'WAIT');
            case 'FAILED_OR_CANCEL': return trades.filter(t => t.status === 'FAILED' || t.status === 'CANCEL');
            default: return trades;
        }
    }

    function renderTradeRow(t) {
        const failed = t.status === 'FAILED' || t.status === 'CANCEL';
        const rowStyle = failed ? 'border-left: 3px solid #dc2626;' : '';
        const rowClass = 'border-t border-gray-200 dark:border-gray-700';

        const statusBadge = ({
            DONE: 'badge-ok', WAIT: 'badge-warning',
            FAILED: 'badge-critical', CANCEL: 'badge-neutral'
        }[t.status] || 'badge-neutral');

        const typeBadge = t.type === 'BUY' ? 'badge-ok' : 'badge-warning';

        return `
            <tr class="${rowClass}" style="${rowStyle}">
                <td class="py-3 px-2">${formatDateTime(t.createdAt)}</td>
                <td class="py-3 px-2"><span class="badge ${typeBadge}">${escapeHtml(t.type)}</span></td>
                <td class="py-3 px-2 text-right">${t.price ? t.price.toLocaleString() : '-'}</td>
                <td class="py-3 px-2 text-right">${t.volume ? t.volume.toFixed(4) : '-'}</td>
                <td class="py-3 px-2 text-right">${t.amount ? Math.round(t.amount).toLocaleString() : '-'}</td>
                <td class="py-3 px-2"><span class="badge ${statusBadge}">${escapeHtml(t.status)}</span></td>
            </tr>
        `;
    }

    async function loadPositions() {
        try {
            const positions = await TradingFetch.json('/api/trading/positions?size=50');
            const tbody = document.getElementById('positions-table');
            if (positions.length === 0) {
                tbody.innerHTML = `<tr><td colspan="6" class="text-center py-8 text-gray-500">No positions</td></tr>`;
                return;
            }
            tbody.innerHTML = positions.map(p => `
                <tr class="border-t border-gray-200 dark:border-gray-700">
                    <td class="py-3 px-2">${p.openedAt ? formatDateTime(p.openedAt) : '-'}</td>
                    <td class="py-3 px-2">${p.closedAt ? formatDateTime(p.closedAt) : '-'}</td>
                    <td class="py-3 px-2 text-right">${p.entryPrice ? p.entryPrice.toLocaleString() : '-'}</td>
                    <td class="py-3 px-2 text-right">${p.exitPrice ? p.exitPrice.toLocaleString() : '-'}</td>
                    <td class="py-3 px-2 text-right ${(p.realizedPnlPct || 0) >= 0 ? 'positive' : 'negative'}">
                        ${p.realizedPnl != null
                            ? signed(Math.round(p.realizedPnl)) + ' (' + signed(p.realizedPnlPct.toFixed(2)) + '%)'
                            : '-'}
                    </td>
                    <td class="py-3 px-2">${reasonBadge(p.closeReason || p.status)}</td>
                </tr>
            `).join('');
        } catch (e) { console.error('positions fail', e); }
    }

    function reasonBadge(reason) {
        if (!reason) return '<span class="badge badge-neutral">-</span>';
        const map = {
            STOP_LOSS: 'badge-critical', TAKE_PROFIT: 'badge-ok',
            TRAILING_STOP: 'badge-notice', SIGNAL: 'badge-notice',
            REBALANCE: 'badge-neutral', MANUAL: 'badge-warning',
            EMERGENCY: 'badge-critical', OPEN: 'badge-ok'
        };
        return `<span class="badge ${map[reason] || 'badge-neutral'}">${escapeHtml(reason)}</span>`;
    }

    async function loadDailyChart() {
        try {
            const data = await TradingFetch.json('/api/trading/profit/daily?days=30');
            if (!data || data.length === 0) return;
            const container = document.getElementById('daily-chart');
            if (typeof LightweightCharts === 'undefined') return;

            dailyChart = LightweightCharts.createChart(container, {
                layout: { background: { type: 'solid', color: '#1f2937' }, textColor: '#d1d5db' },
                grid: { vertLines: { color: '#374151' }, horzLines: { color: '#374151' } },
                rightPriceScale: { borderColor: '#374151' },
                timeScale: { borderColor: '#374151' }
            });
            const series = dailyChart.addHistogramSeries({ priceFormat: { type: 'volume' } });
            const chartData = data.map(d => ({
                time: d.date,
                value: d.realizedPnl,
                color: d.realizedPnl >= 0 ? '#22c55e' : '#ef4444'
            })).reverse();
            series.setData(chartData);
        } catch (e) { console.error('daily chart fail', e); }
    }

    // === helpers ===
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
        if (!s) return '-';
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
