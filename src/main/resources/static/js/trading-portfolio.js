/**
 * trading-portfolio.js
 * Portfolio + Rebalancing 페이지
 */
(function () {
    'use strict';

    let lastStatus = null;

    document.addEventListener('DOMContentLoaded', () => {
        loadAll();
        setInterval(loadAll, 15000);
        document.getElementById('btn-rebalance-refresh')?.addEventListener('click', loadAll);
        document.getElementById('btn-rebalance-now')?.addEventListener('click', onRebalanceNow);
    });

    async function loadAll() {
        await Promise.all([loadStatus(), loadHoldings(), loadRebalanceHistory()]);
    }

    async function loadStatus() {
        try {
            const s = await TradingFetch.json('/api/trading/rebalance/status');
            lastStatus = s;
            renderAllocation(s);
        } catch (e) {
            console.error('rebalance status fail', e);
            document.getElementById('alloc-regime').textContent = '상태 조회 실패';
        }
    }

    function renderAllocation(s) {
        const card = document.getElementById('allocation-card');
        const cur = s.currentRatio != null ? s.currentRatio * 100 : null;
        const tgt = s.targetRatio != null ? s.targetRatio * 100 : null;
        const dev = s.deviation != null ? s.deviation * 100 : null;
        const trig = s.deviationTrigger != null ? s.deviationTrigger * 100 : 10;

        // regime
        const regimeText = ({
            BULL: 'BULL (현재가 > MA60)',
            BEAR: 'BEAR (현재가 < MA60)',
            NEUTRAL: 'NEUTRAL',
            INSUFFICIENT_DATA: 'MA60 데이터 부족 (목표 비중 추정)',
            UNKNOWN: '거래소 응답 없음'
        }[s.marketRegime] || s.marketRegime);
        const regimeBadge = s.marketRegime === 'BULL' ? 'badge-ok'
                         : s.marketRegime === 'BEAR' ? 'badge-warning'
                         : s.marketRegime === 'INSUFFICIENT_DATA' ? 'badge-neutral' : 'badge-neutral';
        document.getElementById('alloc-regime').innerHTML =
            `<span class="badge ${regimeBadge}">${escapeHtml(regimeText)}</span>` +
            (s.currentPrice != null && s.ma60 != null
                ? ` · price ${Math.round(s.currentPrice).toLocaleString()} / MA60 ${Math.round(s.ma60).toLocaleString()}`
                : '');

        document.getElementById('alloc-current-pct').textContent = cur != null ? cur.toFixed(1) + '%' : '-';
        document.getElementById('alloc-target-pct').textContent  = tgt != null ? tgt.toFixed(1) + '% (target)' : '-';
        if (cur != null) document.getElementById('alloc-current-bar').style.width = cur.toFixed(1) + '%';
        if (tgt != null) document.getElementById('alloc-target-bar').style.width  = tgt.toFixed(1) + '%';

        const devEl = document.getElementById('alloc-deviation');
        if (dev != null) {
            const overTrig = Math.abs(dev) >= trig;
            const sign = dev > 0 ? '+' : '';
            devEl.innerHTML = `<span class="${overTrig ? 'negative' : ''}">${sign}${dev.toFixed(2)}%</span> ` +
                (overTrig
                    ? `<span class="badge badge-warning">조정 필요 (>${trig.toFixed(0)}%)</span>`
                    : `<span class="badge badge-ok">밸런스</span>`);
            card.className = 'card mb-6 ' + (overTrig ? 'card-warning' : 'card-ok');
        } else {
            devEl.textContent = '-';
            card.className = 'card mb-6';
        }

        // cooldown
        const cdEl = document.getElementById('alloc-cooldown');
        const remain = s.cooldownRemainingSec || 0;
        if (remain > 0) {
            const h = Math.floor(remain / 3600);
            const m = Math.floor((remain % 3600) / 60);
            const sec = remain % 60;
            cdEl.textContent = h > 0
                ? `${h}h ${m}m`
                : (m > 0 ? `${m}m ${sec}s` : `${sec}s`);
            cdEl.className = 'font-medium negative';
        } else {
            cdEl.textContent = '준비 완료';
            cdEl.className = 'font-medium positive';
        }

        // button enable/disable
        const btn = document.getElementById('btn-rebalance-now');
        const help = document.getElementById('rebalance-help');
        const disabled = !s.enabled || remain > 0;
        btn.disabled = disabled;
        if (!s.enabled) {
            help.textContent = '리밸런싱 기능이 비활성화 상태입니다 (Settings 확인).';
        } else if (remain > 0) {
            help.textContent = '쿨다운 중입니다. 남은 시간이 지난 후 실행 가능합니다.';
        } else if (dev == null || Math.abs(dev) < trig) {
            help.textContent = '편차가 임계 미만입니다. 실행해도 변동이 없을 수 있습니다.';
        } else {
            help.textContent = '실행 가능. 클릭 시 확인 모달이 열립니다.';
        }
    }

    async function loadHoldings() {
        try {
            const s = await TradingFetch.json('/api/trading/profit/summary');
            const tbody = document.getElementById('holdings-body');
            const coinValue = s.coinBalance * s.currentPrice;
            const total = s.totalValue || (s.krwBalance + coinValue);
            const coinWeight = total > 0 ? (coinValue / total * 100) : 0;
            const cashWeight = 100 - coinWeight;

            tbody.innerHTML = `
                <tr class="border-t border-gray-200 dark:border-gray-700">
                    <td class="py-2 dark:text-white">Coin</td>
                    <td class="py-2 text-right">${(s.coinBalance || 0).toFixed(4)}</td>
                    <td class="py-2 text-right">${formatKRW(s.currentPrice)}</td>
                    <td class="py-2 text-right">${formatKRW(coinValue)}</td>
                    <td class="py-2 text-right">${coinWeight.toFixed(1)}%</td>
                </tr>
                <tr class="border-t border-gray-200 dark:border-gray-700">
                    <td class="py-2 dark:text-white">KRW (Cash)</td>
                    <td class="py-2 text-right">${formatKRW(s.krwBalance)}</td>
                    <td class="py-2 text-right">-</td>
                    <td class="py-2 text-right">${formatKRW(s.krwBalance)}</td>
                    <td class="py-2 text-right">${cashWeight.toFixed(1)}%</td>
                </tr>
                <tr class="border-t border-gray-300 dark:border-gray-600 font-medium">
                    <td class="py-2 dark:text-white">Total</td>
                    <td class="py-2 text-right">-</td>
                    <td class="py-2 text-right">-</td>
                    <td class="py-2 text-right dark:text-white">${formatKRW(total)}</td>
                    <td class="py-2 text-right">100%</td>
                </tr>
            `;
        } catch (e) { console.error('holdings fail', e); }
    }

    async function loadRebalanceHistory() {
        try {
            const trades = await TradingFetch.json('/api/trading/trades?size=50');
            const tbody = document.getElementById('rebalance-history-body');
            // /api/trading/trades 는 reason 필드를 노출하지 않음. type/status 위주로 모두 표시하되 안내 텍스트로 한정
            // 향후 백엔드에 reason 필터가 생기면 교체. 지금은 단순히 최근 거래 일부 표시.
            if (!trades || trades.length === 0) {
                tbody.innerHTML = '<tr><td colspan="5" class="text-center py-4 text-gray-500">거래 없음</td></tr>';
                return;
            }
            tbody.innerHTML = trades.slice(0, 15).map(t => `
                <tr class="border-t border-gray-200 dark:border-gray-700">
                    <td class="py-2">${formatDateTime(t.createdAt)}</td>
                    <td class="py-2">
                        <span class="badge ${t.type === 'BUY' ? 'badge-ok' : 'badge-warning'}">${t.type}</span>
                    </td>
                    <td class="py-2 text-right">${t.price ? t.price.toLocaleString() : '-'}</td>
                    <td class="py-2 text-right">${t.amount ? Math.round(t.amount).toLocaleString() : '-'}</td>
                    <td class="py-2">${statusBadge(t.status)}</td>
                </tr>
            `).join('');
        } catch (e) { console.error('history fail', e); }
    }

    function onRebalanceNow() {
        if (!lastStatus) return;
        const cur = lastStatus.currentRatio != null ? (lastStatus.currentRatio * 100).toFixed(1) : '?';
        const tgt = lastStatus.targetRatio != null ? (lastStatus.targetRatio * 100).toFixed(1) : '?';
        const dev = lastStatus.deviation != null ? (lastStatus.deviation * 100).toFixed(2) : '?';
        const direction = lastStatus.deviation != null && lastStatus.deviation < 0 ? '코인 매수' : '코인 매도';

        TradingConfirm.show({
            title: '리밸런싱 실행',
            body: `현재 ${cur}% → 목표 ${tgt}% (${direction} 방향). 체결되면 8시간 쿨다운이 시작됩니다.`,
            impact: `편차 ${dev}% · 시장가 슬리피지/수수료 발생 · 매도 시 평균 손익률 +3% 미만이면 자동 스킵`,
            confirmText: 'REBALANCE',
            countdownSec: 3,
            danger: false,
            confirmLabel: 'Execute',
            onConfirm: async () => {
                const r = await fetch('/api/trading/rebalance/execute', { method: 'POST' });
                const data = await r.json();
                TradingToast.push(data.executed ? 'ok' : 'warning',
                    data.message || (data.executed ? 'Rebalance executed' : 'Skipped'));
                setTimeout(loadAll, 800);
            }
        });
    }

    // === helpers ===
    function formatKRW(v) {
        if (v == null) return '-';
        return Math.round(v).toLocaleString();
    }
    function formatDateTime(s) {
        if (!s) return '-';
        const d = new Date(s);
        return d.toLocaleDateString('ko-KR') + ' ' +
               d.toLocaleTimeString('ko-KR', { hour:'2-digit', minute:'2-digit' });
    }
    function statusBadge(st) {
        const map = { DONE:'badge-ok', WAIT:'badge-warning', CANCEL:'badge-neutral', FAILED:'badge-critical' };
        return `<span class="badge ${map[st] || 'badge-neutral'}">${escapeHtml(st)}</span>`;
    }
    function escapeHtml(s) {
        if (s == null) return '';
        return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;')
            .replace(/"/g,'&quot;').replace(/'/g,'&#39;');
    }
})();
