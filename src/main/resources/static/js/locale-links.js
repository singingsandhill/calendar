/**
 * locale-links.js
 *
 * 클라이언트 측 lang 보존 헬퍼.
 *  - 현재 URL 의 ?lang= 값이 있으면 내부 이동 URL 에 자동 부착
 *  - 외부/스킴 링크 그대로
 *  - 이미 lang= 가 있으면 그대로
 *  - hash(#) 앞에 lang 삽입
 *
 * 사용:
 *   window.location.href = window.localeUrl('/path');
 *   const link = window.localeUrl('/' + ownerId);
 */
(function () {
    'use strict';

    var DEFAULT_LANGUAGE = 'ko';

    function isExternal(url) {
        return /^(https?:)?\/\//.test(url) || /^(mailto:|tel:)/.test(url);
    }

    function currentLang() {
        try {
            var fromUrl = new URLSearchParams(window.location.search).get('lang');
            if (fromUrl) return fromUrl;
        } catch (e) {
            // URLSearchParams unavailable — fall through
        }
        // Cookie fallback so JS-driven nav still preserves lang
        var match = document.cookie.match(/(?:^|;\s*)lang=([^;]+)/);
        return match ? decodeURIComponent(match[1]) : null;
    }

    window.localeUrl = function (url) {
        if (!url || isExternal(url)) return url;
        var lang = currentLang();
        if (!lang || lang === DEFAULT_LANGUAGE) return url;
        if (/[?&]lang=[^&#]*/.test(url)) return url;

        var hashIdx = url.indexOf('#');
        var base = hashIdx >= 0 ? url.slice(0, hashIdx) : url;
        var fragment = hashIdx >= 0 ? url.slice(hashIdx) : '';
        var sep = base.indexOf('?') >= 0 ? '&' : '?';
        return base + sep + 'lang=' + encodeURIComponent(lang) + fragment;
    };
})();
