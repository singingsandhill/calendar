(function() {
    'use strict';

    document.addEventListener('DOMContentLoaded', function() {
        var navbarToggle = document.getElementById('navbarToggle');
        var navMenu = document.getElementById('navMenu');

        if (!navbarToggle || !navMenu) return;

        // Use existing overlay or create one
        var overlay = document.getElementById('navOverlay') || document.querySelector('.nav-overlay');
        if (!overlay) {
            overlay = document.createElement('div');
            overlay.className = 'nav-overlay';
            document.body.appendChild(overlay);
        }

        function toggleMenu() {
            var isOpen = navMenu.classList.toggle('open');
            navbarToggle.classList.toggle('active');
            overlay.classList.toggle('show');
            navbarToggle.setAttribute('aria-expanded', isOpen);
            document.body.style.overflow = isOpen ? 'hidden' : '';
        }

        function closeMenu() {
            navMenu.classList.remove('open');
            navbarToggle.classList.remove('active');
            overlay.classList.remove('show');
            navbarToggle.setAttribute('aria-expanded', 'false');
            document.body.style.overflow = '';
        }

        navbarToggle.addEventListener('click', toggleMenu);
        overlay.addEventListener('click', closeMenu);

        // Close menu when clicking any menu item (links, kakao login, logout submit)
        // — .nav-link 한정이었을 때 minimal 헤더(.nav-link-animated)·카카오 앵커가
        //   같은 페이지 앵커(/#start-form) 이동 시 메뉴+스크롤락을 남기던 문제 해소
        var navLinks = navMenu.querySelectorAll('a, button[type="submit"]');
        for (var i = 0; i < navLinks.length; i++) {
            navLinks[i].addEventListener('click', closeMenu);
        }

        // Close menu on ESC key
        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape' && navMenu.classList.contains('open')) {
                closeMenu();
            }
        });

        // Close menu on window resize (above tablet breakpoint)
        window.addEventListener('resize', function() {
            if (window.innerWidth > 768 && navMenu.classList.contains('open')) {
                closeMenu();
            }
        });
    });
})();
