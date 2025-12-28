/**
 * Garriock Style Animations
 * - Navbar scroll effect
 * - Intersection Observer for scroll animations
 */

document.addEventListener('DOMContentLoaded', () => {
    // Navbar scroll effect
    initNavbarScroll();

    // Scroll animations
    initScrollAnimations();
});

/**
 * Navbar scroll effect - transparent to solid background
 */
function initNavbarScroll() {
    const navbar = document.getElementById('navbarMinimal');
    if (!navbar) return;

    const scrollThreshold = 50;

    function handleScroll() {
        if (window.scrollY > scrollThreshold) {
            navbar.classList.add('scrolled');
        } else {
            navbar.classList.remove('scrolled');
        }
    }

    // Initial check
    handleScroll();

    // Throttled scroll handler
    let ticking = false;
    window.addEventListener('scroll', () => {
        if (!ticking) {
            window.requestAnimationFrame(() => {
                handleScroll();
                ticking = false;
            });
            ticking = true;
        }
    }, { passive: true });
}

/**
 * Intersection Observer for scroll animations
 * Elements with .animate-on-scroll class will animate when visible
 */
function initScrollAnimations() {
    const animateElements = document.querySelectorAll('.animate-on-scroll');

    if (animateElements.length === 0) return;

    // Check for reduced motion preference
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
        animateElements.forEach(el => el.classList.add('animate-in'));
        return;
    }

    // 각 요소에 인덱스 할당 (DOM 순서 기준)
    animateElements.forEach((el, index) => {
        el.dataset.animationIndex = index;
    });

    const observerOptions = {
        threshold: 0.1,
        rootMargin: '0px 0px -50px 0px'
    };

    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                // data 속성에서 인덱스 가져오기
                const index = parseInt(entry.target.dataset.animationIndex, 10);
                const delay = index * 100;

                setTimeout(() => {
                    entry.target.classList.add('animate-in');
                }, delay);

                // Stop observing once animated
                observer.unobserve(entry.target);
            }
        });
    }, observerOptions);

    animateElements.forEach(el => observer.observe(el));
}
