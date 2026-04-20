import { renderCalendar, resetSelections, saveSelections } from './calendar.js';
import {
    bindParticipantForm,
    openAddParticipantModal,
    closeAddParticipantModal
} from './participants.js';
import { bindVotingInputs, addLocation, voteLocation, addMenu, voteMenu } from './voting.js';
import { copyLink } from './utils.js';

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
    'onboarding.dismiss': () => document.getElementById('onboardingBanner')?.remove()
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
    document.addEventListener('click', dispatch);
});
