/**
 * Announcement Image Generator
 * Uses HTML Canvas API for client-side image generation
 */

(function() {
    'use strict';

    // Canvas dimensions (square for social media)
    const CANVAS_WIDTH = 1080;
    const CANVAS_HEIGHT = 1080;

    // Logo path
    const LOGO_PATH = '/image/crew_logo.png';

    // DOM Elements
    let canvas, ctx;
    let logoImage = null;
    let isLogoLoaded = false;

    // Initialize on DOM ready
    document.addEventListener('DOMContentLoaded', init);

    function init() {
        canvas = document.getElementById('announceCanvas');
        if (!canvas) return;

        ctx = canvas.getContext('2d');

        // Set canvas size
        canvas.width = CANVAS_WIDTH;
        canvas.height = CANVAS_HEIGHT;

        // Preload logo
        loadLogo();

        // Event listeners
        document.getElementById('generateBtn').addEventListener('click', generateImage);
        document.getElementById('downloadBtn').addEventListener('click', downloadImage);

        // Real-time preview on input change
        const inputs = ['announceDate', 'announceTime', 'announceLocation'];
        inputs.forEach(id => {
            const el = document.getElementById(id);
            if (el) {
                el.addEventListener('input', debounce(generateImage, 300));
            }
        });

        // Initial render (blank template)
        drawBlankTemplate();
    }

    function loadLogo() {
        logoImage = new Image();
        logoImage.onload = function() {
            isLogoLoaded = true;
            generateImage(); // Re-render with logo
        };
        logoImage.onerror = function() {
            console.warn('Logo failed to load');
            isLogoLoaded = false;
        };
        logoImage.src = LOGO_PATH;
    }

    function drawBlankTemplate() {
        // White background
        ctx.fillStyle = '#FFFFFF';
        ctx.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);

        // Draw logo if loaded
        if (isLogoLoaded) {
            drawLogo();
        }

        // Placeholder text
        ctx.fillStyle = '#CCCCCC';
        ctx.font = '24px "Noto Sans KR", sans-serif';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText('정보를 입력하면 미리보기가 표시됩니다', CANVAS_WIDTH / 2, CANVAS_HEIGHT / 2 + 150);
    }

    function generateImage() {
        const dateEl = document.getElementById('announceDate');
        const timeEl = document.getElementById('announceTime');
        const locationEl = document.getElementById('announceLocation');

        // DOM 요소가 없으면 중단
        if (!dateEl || !timeEl || !locationEl) return;

        const date = dateEl.value;
        const time = timeEl.value;
        const location = locationEl.value;

        // Clear canvas with white background
        ctx.fillStyle = '#FFFFFF';
        ctx.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);

        // Draw logo at top (large)
        if (isLogoLoaded) {
            drawLogo();
        }

        // Draw info fields
        if (date || time || location) {
            drawInfoSection(date, time, location);
            document.getElementById('downloadBtn').disabled = false;
        } else {
            document.getElementById('downloadBtn').disabled = true;
            ctx.fillStyle = '#999999';
            ctx.font = '24px "Noto Sans KR", sans-serif';
            ctx.textAlign = 'center';
            ctx.textBaseline = 'middle';
            ctx.fillText('날짜, 시간, 장소를 입력하세요', CANVAS_WIDTH / 2, 850);
        }

        // Draw footer/watermark
        drawFooter();
    }

    function drawLogo() {
        // Center logo, large size covering most of canvas
        const maxWidth = 1000;
        const maxHeight = 900;

        let width = logoImage.width;
        let height = logoImage.height;

        // Scale to fit within max dimensions
        if (width > maxWidth) {
            height = (maxWidth / width) * height;
            width = maxWidth;
        }
        if (height > maxHeight) {
            width = (maxHeight / height) * width;
            height = maxHeight;
        }

        const x = (CANVAS_WIDTH - width) / 2;
        const y = 60;

        ctx.drawImage(logoImage, x, y, width, height);
    }

    function drawInfoSection(date, time, location) {
        const startY = 720;
        const lineHeight = 70;

        ctx.fillStyle = '#333333';
        ctx.font = '36px "Noto Sans KR", sans-serif';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';

        let currentY = startY;

        // Format date for Korean display
        if (date) {
            const formattedDate = formatDateKorean(date);
            ctx.fillText(formattedDate, CANVAS_WIDTH / 2, currentY);
            currentY += lineHeight;
        }

        // Format time
        if (time) {
            const formattedTime = formatTimeKorean(time);
            ctx.fillText(formattedTime, CANVAS_WIDTH / 2, currentY);
            currentY += lineHeight;
        }

        // Location
        if (location) {
            ctx.fillText(location, CANVAS_WIDTH / 2, currentY);
        }
    }

    function drawFooter() {
        // Subtle footer with accent color
        ctx.fillStyle = '#00ff88'; // Accent color
        ctx.font = 'bold 28px "Noto Sans KR", sans-serif';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText('97 Runners', CANVAS_WIDTH / 2, CANVAS_HEIGHT - 60);
    }

    function formatDateKorean(dateStr) {
        const date = new Date(dateStr);
        const days = ['일', '월', '화', '수', '목', '금', '토'];
        const year = date.getFullYear();
        const month = date.getMonth() + 1;
        const day = date.getDate();
        const dayOfWeek = days[date.getDay()];

        return `${year}년 ${month}월 ${day}일 (${dayOfWeek})`;
    }

    function formatTimeKorean(timeStr) {
        const [hours, minutes] = timeStr.split(':').map(Number);
        const period = hours < 12 ? '오전' : '오후';
        const hour12 = hours % 12 || 12;

        if (minutes === 0) {
            return `${period} ${hour12}시`;
        }
        return `${period} ${hour12}시 ${minutes}분`;
    }

    function downloadImage() {
        const date = document.getElementById('announceDate').value;

        // Generate filename
        const filename = `97runners_${date || 'announce'}.png`;

        // Create download link
        const link = document.createElement('a');
        link.download = filename;
        link.href = canvas.toDataURL('image/png');
        link.click();
    }

    // Utility: Debounce function for real-time preview
    function debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    }
})();
