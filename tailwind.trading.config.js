/** Tailwind 빌드 설정 — 트레이딩 페이지 전용 유틸리티 CSS 생성
 *  npx tailwindcss@3.4 -c tailwind.trading.config.js -o src/main/resources/static/css/trading-tw.css --minify
 */
module.exports = {
  content: [
    './src/main/resources/templates/trading/**/*.html',
    './src/main/resources/templates/stock/**/*.html',
    './src/main/resources/static/js/trading-*.js',
  ],
  darkMode: 'media',
  theme: { extend: {} },
  corePlugins: { preflight: true },
};
