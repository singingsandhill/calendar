import { messages } from './state.js';

export function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

export function copyLink() {
    const url = window.location.href;
    if (navigator.clipboard && window.isSecureContext) {
        navigator.clipboard.writeText(url)
            .then(() => alert(messages.linkCopied))
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
        alert(messages.linkCopied);
    } catch (err) {
        prompt(messages.linkCopyPrompt, text);
    }
    document.body.removeChild(textarea);
}
