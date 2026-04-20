export function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

export function copyLink() {
    const url = window.location.href;
    if (navigator.clipboard && window.isSecureContext) {
        navigator.clipboard.writeText(url)
            .then(() => alert('링크가 클립보드에 복사되었습니다!'))
            .catch(() => fallbackCopy(url));
    } else {
        fallbackCopy(url);
    }
}

function fallbackCopy(text) {
    const textarea = document.createElement('textarea');
    textarea.value = text;
    textarea.style.position = 'fixed';
    textarea.style.opacity = '0';
    document.body.appendChild(textarea);
    textarea.select();
    try {
        document.execCommand('copy');
        alert('링크가 클립보드에 복사되었습니다!');
    } catch (err) {
        prompt('아래 링크를 복사하세요:', text);
    }
    document.body.removeChild(textarea);
}
