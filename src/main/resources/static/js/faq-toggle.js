/**
 * FAQ 아코디언 토글 — .faq-item.open 클래스 + aria-expanded (ADR datedate/frontend/0004).
 * 열림 표시(× 회전)·높이 전환은 style.css 의 .faq-item.open 규칙이 담당한다.
 *
 * 사용: FAQ 가 있는 페이지에서
 *   <script defer th:src="@{/js/faq-toggle.js}"></script>
 */
(function () {
    'use strict';

    var buttons = document.querySelectorAll('.faq-question');
    for (var i = 0; i < buttons.length; i++) {
        buttons[i].addEventListener('click', function () {
            var isOpen = this.parentElement.classList.toggle('open');
            this.setAttribute('aria-expanded', isOpen ? 'true' : 'false');
        });
    }
})();
