import { renderCalendar, resetSelections, saveSelections } from './calendar.js';
import {
    bindParticipantForm,
    openAddParticipantModal,
    closeAddParticipantModal
} from './participants.js';
import { bindVotingInputs, addLocation, voteLocation, addMenu, voteMenu } from './voting.js';
import { copyLink } from './utils.js';

const ONBOARDING_DISMISSED_KEY = 'datedate.onboarding.dismissed';

function lsGet(key) {
    try { return localStorage.getItem(key); } catch { return null; }
}
function lsSet(key, value) {
    try { localStorage.setItem(key, value); } catch { /* private mode 등 */ }
}
function lsRemove(key) {
    try { localStorage.removeItem(key); } catch { /* noop */ }
}

function setBannerOpen(open) {
    const banner = document.getElementById('onboardingBanner');
    const toggle = document.getElementById('onboardingHelpToggle');
    if (!banner) return;
    banner.hidden = !open;
    if (toggle) {
        toggle.hidden = open;
        toggle.setAttribute('aria-expanded', open ? 'true' : 'false');
    }
}

function initOnboardingBanner() {
    const banner = document.getElementById('onboardingBanner');
    if (!banner) return;
    const dismissed = lsGet(ONBOARDING_DISMISSED_KEY) === 'true';
    const defaultOpen = banner.dataset.defaultOpen === 'true';
    setBannerOpen(!dismissed && defaultOpen);
}

const actions = {
    'selections.reset': () => resetSelections(),
    'selections.save': () => saveSelections(),
    'participant.openModal': () => openAddParticipantModal(),
    'participant.closeModal': () => closeAddParticipantModal(),
    'link.copy': () => copyLink(),
    'location.add': () => addLocation(),
    'location.vote': (target) => voteLocation(target),
    'menu.add': () => addMenu(),
    'menu.vote': (target) => voteMenu(target),
    'onboarding.dismiss': () => {
        setBannerOpen(false);
        lsSet(ONBOARDING_DISMISSED_KEY, 'true');
        document.getElementById('onboardingHelpToggle')?.focus();
    },
    'onboarding.show': () => {
        setBannerOpen(true);
        lsRemove(ONBOARDING_DISMISSED_KEY);
    }
};

function dispatch(event) {
    const target = event.target.closest('[data-action]');
    if (!target) return;
    const action = actions[target.dataset.action];
    if (!action) return;
    event.preventDefault();
    action(target);
}

document.addEventListener('DOMContentLoaded', () => {
    renderCalendar();
    bindParticipantForm();
    bindVotingInputs();
    initOnboardingBanner();
    document.addEventListener('click', dispatch);
});
