import { schedule, locations, menus, messages, formatVotes } from './state.js';
import { escapeHtml } from './utils.js';

function getSelectedVoterName() {
    const select = document.getElementById('participantSelect');
    if (!select.value) {
        alert(messages.selectName);
        return null;
    }
    return select.options[select.selectedIndex].text;
}

function updateItemUI(itemElement, item) {
    itemElement.querySelector('.location-votes').textContent = formatVotes(item.voteCount);
    const votersDiv = itemElement.querySelector('.location-voters');
    votersDiv.innerHTML = item.voters.map(v =>
        `<span class="voter-tag">${escapeHtml(v)}</span>`
    ).join('');
}

async function toggleVoteFor(item, voterName, voteApi, unvoteApi) {
    const hasVoted = item.voters.some(v => v.toLowerCase() === voterName.toLowerCase());
    if (hasVoted) {
        await unvoteApi(item.id, voterName);
        item.voters = item.voters.filter(v => v.toLowerCase() !== voterName.toLowerCase());
        item.voteCount--;
    } else {
        await voteApi(item.id, voterName);
        item.voters.push(voterName);
        item.voteCount++;
    }
}

// ==================== Location ====================

export async function addLocation() {
    const input = document.getElementById('locationInput');
    const name = input.value.trim();
    if (!name) {
        alert(messages.locationRequired);
        return;
    }
    try {
        const newLocation = await window.api.addLocation(schedule.scheduleId, name);
        locations.push(newLocation);
        addLocationToList(newLocation);
        input.value = '';
        window.dataLayer = window.dataLayer || [];
        window.dataLayer.push({
            event: 'location_added',
            schedule_id: schedule.scheduleId,
            location_count_after: document.querySelectorAll('#locationList .location-item').length
        });
    } catch (error) {
        window.toast.error(error.message);
    }
}

function addLocationToList(location) {
    const list = document.getElementById('locationList');
    const emptyMsg = list.querySelector('.empty-locations');
    if (emptyMsg) emptyMsg.remove();

    const item = document.createElement('div');
    item.className = 'location-item';
    item.innerHTML = `
        <div class="location-info">
            <span class="location-name">${escapeHtml(location.name)}</span>
            <span class="location-votes">${escapeHtml(formatVotes(location.voteCount))}</span>
        </div>
        <div class="location-voters"></div>
        <div class="location-actions">
            <button class="btn btn-sm btn-primary vote-btn"
                    data-action="location.vote"
                    data-location-id="${location.id}">${escapeHtml(messages.voteLabel)}</button>
        </div>
    `;
    list.appendChild(item);
}

export async function voteLocation(button) {
    const locationId = parseInt(button.dataset.locationId);
    const voterName = getSelectedVoterName();
    if (!voterName) return;

    const location = locations.find(l => l.id === locationId);
    if (!location) return;

    const wasVoted = location.voters.some(v => v.toLowerCase() === voterName.toLowerCase());

    try {
        await toggleVoteFor(
            location,
            voterName,
            window.api.voteLocation.bind(window.api),
            window.api.unvoteLocation.bind(window.api)
        );
        window.dataLayer = window.dataLayer || [];
        window.dataLayer.push({
            event: 'vote_cast',
            target: 'location',
            target_id: locationId,
            action: wasVoted ? 'unvote' : 'vote'
        });
        updateItemUI(button.closest('.location-item'), location);
    } catch (error) {
        window.toast.error(error.message);
    }
}

// ==================== Menu ====================

export async function addMenu() {
    const nameInput = document.getElementById('menuInput');
    const urlInput = document.getElementById('menuUrlInput');
    const name = nameInput.value.trim();
    const url = urlInput.value.trim() || null;

    if (!name) {
        alert(messages.menuRequired);
        return;
    }
    try {
        const newMenu = await window.api.addMenu(schedule.scheduleId, name, url);
        menus.push(newMenu);
        addMenuToList(newMenu);
        nameInput.value = '';
        urlInput.value = '';
        window.dataLayer = window.dataLayer || [];
        window.dataLayer.push({
            event: 'menu_added',
            schedule_id: schedule.scheduleId,
            menu_count_after: document.querySelectorAll('#menuList .location-item').length
        });
    } catch (error) {
        window.toast.error(error.message);
    }
}

function addMenuToList(menu) {
    const list = document.getElementById('menuList');
    const emptyMsg = list.querySelector('.empty-locations');
    if (emptyMsg) emptyMsg.remove();

    const item = document.createElement('div');
    item.className = 'location-item';
    const urlLink = menu.url
        ? `<a href="${escapeHtml(menu.url)}" target="_blank" rel="noopener noreferrer" class="menu-link">🔗</a>`
        : '';
    item.innerHTML = `
        <div class="location-info">
            <span class="location-name">${escapeHtml(menu.name)}</span>
            ${urlLink}
            <span class="location-votes">${escapeHtml(formatVotes(menu.voteCount))}</span>
        </div>
        <div class="location-voters"></div>
        <div class="location-actions">
            <button class="btn btn-sm btn-primary menu-vote-btn"
                    data-action="menu.vote"
                    data-menu-id="${menu.id}">${escapeHtml(messages.voteLabel)}</button>
        </div>
    `;
    list.appendChild(item);
}

export async function voteMenu(button) {
    const menuId = parseInt(button.dataset.menuId);
    const voterName = getSelectedVoterName();
    if (!voterName) return;

    const menu = menus.find(m => m.id === menuId);
    if (!menu) return;

    const wasVoted = menu.voters.some(v => v.toLowerCase() === voterName.toLowerCase());

    try {
        await toggleVoteFor(
            menu,
            voterName,
            window.api.voteMenu.bind(window.api),
            window.api.unvoteMenu.bind(window.api)
        );
        window.dataLayer = window.dataLayer || [];
        window.dataLayer.push({
            event: 'vote_cast',
            target: 'menu',
            target_id: menuId,
            action: wasVoted ? 'unvote' : 'vote'
        });
        updateItemUI(button.closest('.location-item'), menu);
    } catch (error) {
        window.toast.error(error.message);
    }
}

// ==================== Bind input enter key ====================

export function bindVotingInputs() {
    const locationInput = document.getElementById('locationInput');
    if (locationInput) {
        locationInput.addEventListener('keypress', e => {
            if (e.key === 'Enter') {
                e.preventDefault();
                addLocation();
            }
        });
    }

    const menuInput = document.getElementById('menuInput');
    if (menuInput) {
        menuInput.addEventListener('keypress', e => {
            if (e.key === 'Enter') {
                e.preventDefault();
                addMenu();
            }
        });
    }
}
