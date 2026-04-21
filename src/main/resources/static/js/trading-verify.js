/**
 * trading-verify.js
 * Verify 페이지: 수동 트리거만 (자동 폴링 X). Test Order는 3중 가드.
 */
(function () {
    'use strict';

    document.addEventListener('DOMContentLoaded', () => {
        document.querySelectorAll('button[data-verify]').forEach(btn => {
            btn.addEventListener('click', () => runVerify(btn.dataset.verify));
        });
        document.getElementById('btn-run-full')?.addEventListener('click', runFullCheck);
        document.getElementById('btn-test-order')?.addEventListener('click', onTestOrder);
    });

    async function runVerify(kind) {
        const targetId = kind + '-result';
        const target = document.getElementById(targetId);
        const card = document.getElementById('card-' + kind);
        target.innerHTML = '<div class="text-gray-500">테스트 중…</div>';
        try {
            const data = await TradingFetch.json('/api/trading/verify/' + kind);
            renderResult(kind, data);
            if (card) card.className = 'card ' + ((data.success === false || data.apiKeyConfigured === false) ? 'card-warning' : 'card-ok');
        } catch (e) {
            target.innerHTML = `<div class="negative">실패: ${escapeHtml(e.message)}</div>`;
            if (card) card.className = 'card card-critical';
        }
    }

    function renderResult(kind, data) {
        const target = document.getElementById(kind + '-result');
        const ts = data.timestamp ? new Date(data.timestamp) : new Date();
        const tsLabel = ts.toLocaleTimeString('ko-KR');

        if (kind === 'config') {
            target.innerHTML = kvRows([
                ['API Key', data.apiKeyConfigured
                    ? '<span class="badge badge-ok">configured</span>'
                    : '<span class="badge badge-critical">미설정</span>'],
                ['Base URL', escapeHtml(data.baseUrl || '-')],
                ['Bot Enabled', data.botEnabled
                    ? '<span class="badge badge-ok">true</span>'
                    : '<span class="badge badge-warning">false</span>'],
                ['Market', escapeHtml(data.market || '-')],
                ['Max Positions', data.maxPositions ?? '-'],
                ['Order Ratio', data.orderRatio != null ? (data.orderRatio * 100).toFixed(1) + '%' : '-']
            ]) + freshness(tsLabel) + (data.warning ? warningLine(data.warning) : '');
            return;
        }

        if (kind === 'price') {
            if (data.success === false) {
                target.innerHTML = errorBlock(data) + freshness(tsLabel);
                return;
            }
            target.innerHTML = kvRows([
                ['Ask', data.askPrice != null ? data.askPrice.toLocaleString() : '-'],
                ['Bid', data.bidPrice != null ? data.bidPrice.toLocaleString() : '-'],
                ['Mid', data.midPrice != null ? data.midPrice.toLocaleString() : '-'],
                ['Spread', `${data.spread ?? '-'} (${data.spreadPercent ?? '-'})`],
                ['Depth', data.orderbookDepth ?? '-'],
                ['getCurrentPrice()', data.getCurrentPriceResult != null ? data.getCurrentPriceResult.toLocaleString() : '-']
            ]) + freshness(tsLabel);
            return;
        }

        if (kind === 'balance') {
            if (data.success === false) {
                target.innerHTML = errorBlock(data) + freshness(tsLabel);
                return;
            }
            target.innerHTML = kvRows([
                ['KRW', data.krwBalance != null ? formatKRW(data.krwBalance) + ' KRW' : '-'],
                ['Coin', `${data.coinBalance != null ? data.coinBalance : '-'} ${data.coinCurrency || ''}`],
                ['Order Ratio', data.orderRatio != null ? (data.orderRatio * 100).toFixed(1) + '%' : '-'],
                ['Orderable', data.orderableAmount != null ? formatKRW(data.orderableAmount) + ' KRW' : '-'],
                ['Min Order', formatKRW(data.minOrderAmount || 5000) + ' KRW'],
                ['Can Place Order', data.canPlaceOrder
                    ? '<span class="badge badge-ok">yes</span>'
                    : '<span class="badge badge-warning">no</span>']
            ]) + freshness(tsLabel);
            return;
        }
    }

    async function runFullCheck() {
        const btn = document.getElementById('btn-run-full');
        btn.disabled = true;
        btn.textContent = 'Running…';
        try {
            const data = await TradingFetch.json('/api/trading/verify/full', { timeoutMs: 15000 });
            // 각 섹션 갱신
            if (data.config) renderResult('config', data.config);
            if (data.price) renderResult('price', data.price);
            if (data.balance && !data.balance.skipped) renderResult('balance', data.balance);

            const out = document.getElementById('full-output');
            out.textContent = JSON.stringify(data, null, 2);
            document.getElementById('card-full-output').style.display = '';
            document.getElementById('full-output-time').textContent =
                'fetched ' + new Date().toLocaleTimeString('ko-KR');
            TradingToast.push('ok', 'Full check 완료');
        } catch (e) {
            TradingToast.push('critical', 'Full check 실패: ' + e.message);
        } finally {
            btn.disabled = false;
            btn.textContent = 'Run Full Check';
        }
    }

    function onTestOrder() {
        const amountInput = document.getElementById('test-amount');
        const amount = Math.max(5000, Number(amountInput.value || 5500));
        const immediateSell = document.getElementById('test-immediate-sell').checked;

        TradingConfirm.show({
            title: '⚠ Test Order — 실제 자금 사용',
            body: `Bithumb에서 ${formatKRW(amount)} KRW 만큼 시장가 매수합니다.` +
                  (immediateSell ? ' 매수 직후 즉시 매도하여 원상 복구합니다.' : ' 매수 후 포지션이 그대로 유지됩니다.'),
            impact: '슬리피지·수수료 발생 (taker 0.25%) · DB에 Trade/Position 레코드 생성',
            confirmText: 'TEST',
            countdownSec: 5,
            danger: true,
            confirmLabel: 'Run Test Order',
            onConfirm: async () => {
                const url = `/api/trading/verify/test-order?amount=${amount}&immediatelySell=${immediateSell}`;
                const r = await fetch(url, { method: 'POST' });
                const data = await r.json();
                renderTestOrderResult(data);
                TradingToast.push(data.success ? 'ok' : 'critical',
                    data.success ? 'Test order 완료' : ('Test order 실패: ' + (data.error || 'unknown')));
            }
        });
    }

    function renderTestOrderResult(data) {
        const target = document.getElementById('test-order-result');
        const card = document.getElementById('card-test-order');
        const lines = [];

        lines.push(['Success', data.success
            ? '<span class="badge badge-ok">true</span>'
            : '<span class="badge badge-critical">false</span>']);
        if (data.orderUuid) lines.push(['Order UUID', escapeHtml(data.orderUuid)]);
        if (data.executedPrice != null) lines.push(['Executed Price', data.executedPrice.toLocaleString()]);
        if (data.executedVolume != null) lines.push(['Executed Volume', String(data.executedVolume)]);
        if (data.fee != null) lines.push(['Fee', formatKRW(data.fee) + ' KRW']);
        if (data.positionIdCreated) lines.push(['Position ID', data.positionIdCreated]);
        if (data.sellAttempt) {
            lines.push(['Sell Attempted', data.sellSuccess
                ? '<span class="badge badge-ok">success</span>'
                : (data.sellSkipped
                    ? `<span class="badge badge-warning">skipped: ${escapeHtml(data.sellSkipReason || '')}</span>`
                    : '<span class="badge badge-critical">failed</span>')]);
        }
        if (data.realizedPnl != null) lines.push(['Realized P&L', signed(data.realizedPnl) + ' KRW']);
        if (data.krwBalanceAfter != null) lines.push(['KRW After', formatKRW(data.krwBalanceAfter)]);
        if (data.coinBalanceAfter != null) lines.push(['Coin After', String(data.coinBalanceAfter)]);
        if (data.error) lines.push(['Error', `<span class="negative">${escapeHtml(data.error)}</span>`]);

        target.innerHTML = kvRows(lines);
        card.className = 'card ' + (data.success ? 'card-ok' : 'card-critical');
    }

    // === helpers ===
    function kvRows(rows) {
        return rows.map(([k, v]) =>
            `<div class="flex justify-between gap-4 border-b border-gray-100 dark:border-gray-800 py-1">
                <span class="text-gray-500">${escapeHtml(k)}</span>
                <span class="text-right">${v}</span>
             </div>`
        ).join('');
    }
    function freshness(t) {
        return `<div class="text-xs text-gray-400 mt-2">checked ${t}</div>`;
    }
    function warningLine(msg) {
        return `<div class="mt-2 negative text-xs">⚠ ${escapeHtml(msg)}</div>`;
    }
    function errorBlock(data) {
        return `<div class="negative">${escapeHtml(data.error || 'unknown error')}</div>` +
               (data.errorType ? `<div class="text-xs text-gray-500">${escapeHtml(data.errorType)}</div>` : '');
    }
    function formatKRW(v) {
        if (v == null) return '-';
        return Math.round(Number(v)).toLocaleString();
    }
    function signed(v) {
        const n = Number(v);
        if (isNaN(n)) return v;
        return (n >= 0 ? '+' : '') + Math.round(n).toLocaleString();
    }
    function escapeHtml(s) {
        if (s == null) return '';
        return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;')
            .replace(/"/g,'&quot;').replace(/'/g,'&#39;');
    }
})();
