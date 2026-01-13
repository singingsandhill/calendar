/**
 * trading-dashboard.js
 * Trading bot dashboard functionality
 *
 * Requires: LightweightCharts library to be loaded before this script
 */

(function() {
    'use strict';

    let chart;
    let candleSeries;
    let ma5Line, ma20Line, ma60Line;

    // Initialize when DOM is ready
    document.addEventListener('DOMContentLoaded', () => {
        initChart();
        loadIndicators();
        loadPositions();

        // Auto-refresh intervals
        setInterval(loadIndicators, 10000);
        setInterval(updateBotStatus, 5000);
        setInterval(loadTradeMarkers, 30000);
    });

    // Handle window resize
    window.addEventListener('resize', () => {
        if (chart) {
            chart.applyOptions({ width: document.getElementById('chart-container').clientWidth });
        }
    });

    // ==================== Chart Functions ====================

    function initChart() {
        const container = document.getElementById('chart-container');
        if (!container || typeof LightweightCharts === 'undefined') {
            console.error('Chart container or LightweightCharts not available');
            return;
        }

        chart = LightweightCharts.createChart(container, {
            layout: {
                background: { type: 'solid', color: '#1f2937' },
                textColor: '#d1d5db',
            },
            grid: {
                vertLines: { color: '#374151' },
                horzLines: { color: '#374151' },
            },
            crosshair: {
                mode: LightweightCharts.CrosshairMode.Normal,
            },
            rightPriceScale: {
                borderColor: '#374151',
            },
            timeScale: {
                borderColor: '#374151',
                timeVisible: true,
                secondsVisible: false,
            },
        });

        candleSeries = chart.addCandlestickSeries({
            upColor: '#22c55e',
            downColor: '#ef4444',
            borderDownColor: '#ef4444',
            borderUpColor: '#22c55e',
            wickDownColor: '#ef4444',
            wickUpColor: '#22c55e',
        });

        ma5Line = chart.addLineSeries({ color: '#3b82f6', lineWidth: 1, title: 'MA5' });
        ma20Line = chart.addLineSeries({ color: '#f59e0b', lineWidth: 1, title: 'MA20' });
        ma60Line = chart.addLineSeries({ color: '#8b5cf6', lineWidth: 1, title: 'MA60' });

        loadChartData();
    }

    async function loadChartData() {
        try {
            const response = await fetch('/api/trading/candles?count=200');
            const data = await response.json();

            if (data.candles && data.candles.length > 0) {
                const candleData = data.candles.map(c => ({
                    time: new Date(c.time).getTime() / 1000,
                    open: c.open,
                    high: c.high,
                    low: c.low,
                    close: c.close,
                })).reverse();

                candleSeries.setData(candleData);

                // Update current price
                const lastCandle = candleData[candleData.length - 1];
                document.getElementById('current-price').textContent =
                    lastCandle.close.toLocaleString() + ' KRW';

                // Load trade markers after candles
                await loadTradeMarkers();
            }
        } catch (error) {
            console.error('Failed to load chart data:', error);
        }
    }

    async function loadTradeMarkers() {
        try {
            const response = await fetch('/api/trading/chart/trades?minutes=200');
            const trades = await response.json();

            if (trades && trades.length > 0) {
                const markers = trades.map(t => ({
                    time: new Date(t.time).getTime() / 1000,
                    position: t.type === 'BUY' ? 'belowBar' : 'aboveBar',
                    color: t.type === 'BUY' ? '#22c55e' : '#ef4444',
                    shape: t.type === 'BUY' ? 'arrowUp' : 'arrowDown',
                    text: t.type + ' ' + t.price.toLocaleString()
                }));

                candleSeries.setMarkers(markers);
            }
        } catch (error) {
            console.error('Failed to load trade markers:', error);
        }
    }

    // ==================== Indicators ====================

    async function loadIndicators() {
        try {
            const response = await fetch('/api/trading/ticker');
            const data = await response.json();

            document.getElementById('indicator-rsi').textContent =
                data.rsi ? data.rsi.toFixed(2) : '-';
            document.getElementById('indicator-stoch').textContent =
                data.stochK && data.stochD ?
                    data.stochK.toFixed(1) + ' / ' + data.stochD.toFixed(1) : '-';
            document.getElementById('indicator-ma5').textContent =
                data.ma5 ? data.ma5.toLocaleString() : '-';
            document.getElementById('indicator-ma20').textContent =
                data.ma20 ? data.ma20.toLocaleString() : '-';

            if (data.currentPrice) {
                document.getElementById('current-price').textContent =
                    data.currentPrice.toLocaleString() + ' KRW';
            }
        } catch (error) {
            console.error('Failed to load indicators:', error);
        }
    }

    // ==================== Positions ====================

    async function loadPositions() {
        try {
            const response = await fetch('/api/trading/positions?size=5');
            const positions = await response.json();

            const tbody = document.getElementById('positions-table');
            if (positions.length === 0) {
                tbody.innerHTML = '<tr><td colspan="4" class="text-center py-4 text-gray-500">No positions</td></tr>';
                return;
            }

            tbody.innerHTML = positions.map(p => `
                <tr class="border-t border-gray-700">
                    <td class="py-2">${p.entryPrice ? p.entryPrice.toLocaleString() : '-'}</td>
                    <td class="py-2">${p.exitPrice ? p.exitPrice.toLocaleString() : '-'}</td>
                    <td class="py-2 text-right ${p.realizedPnlPct >= 0 ? 'positive' : 'negative'}">
                        ${p.realizedPnlPct ? p.realizedPnlPct.toFixed(2) + '%' : '-'}
                    </td>
                    <td class="py-2">${p.closeReason || p.status}</td>
                </tr>
            `).join('');
        } catch (error) {
            console.error('Failed to load positions:', error);
        }
    }

    // ==================== Bot Control ====================

    window.toggleBot = async function() {
        const response = await fetch('/api/trading/bot/status');
        const status = await response.json();

        const endpoint = status.running ? '/api/trading/bot/stop' : '/api/trading/bot/start';
        await fetch(endpoint, { method: 'POST' });
        location.reload();
    };

    window.emergencyClose = async function() {
        if (confirm('Are you sure you want to execute emergency close?')) {
            await fetch('/api/trading/bot/emergency-close', { method: 'POST' });
            location.reload();
        }
    };

    async function updateBotStatus() {
        try {
            const response = await fetch('/api/trading/bot/status');
            const status = await response.json();

            const dot = document.getElementById('status-dot');
            const text = document.getElementById('status-text');
            const btn = document.getElementById('toggle-btn');

            if (status.running) {
                dot.className = status.paused ? 'w-3 h-3 rounded-full bg-yellow-500' : 'w-3 h-3 rounded-full bg-green-500';
                text.textContent = status.paused ? 'Paused' : 'Running';
                btn.textContent = 'Stop';
            } else {
                dot.className = 'w-3 h-3 rounded-full bg-red-500';
                text.textContent = 'Stopped';
                btn.textContent = 'Start';
            }
        } catch (error) {
            console.error('Failed to update bot status:', error);
        }
    }

})();
