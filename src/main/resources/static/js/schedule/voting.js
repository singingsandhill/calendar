import { schedule, locations, menus } from './state.js';
import { escapeHtml } from './utils.js';

function getSelectedVoterName() {
    const select = document.getElementById('participantSelect');
    if (!select.value) {
        alert('먼저 이름을 선택하세요');
        return null;
    }
    return select.options[select.selectedIndex].text;
}

function updateItemUI(itemElement, item) {
    itemElement.querySelector('.location-votes').textContent = item.voteCount + '표';
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
        alert('장소를 입력하세요');
        return;
    }
    try {
        const newLocation = await window.api.addLocation(schedule.scheduleId, name);
        locations.push(newLocation);
        addLocationToList(newLocation);
        input.value = '';
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
            <span class="location-votes">${location.voteCount}표</span>
        </div>
        <div class="location-voters"></div>
        <div class="location-actions">
            <button class="btn btn-sm btn-primary vote-btn"
                    data-action="location.vote"
                    data-location-id="${location.id}">투표</button>
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

    try {
        await toggleVoteFor(
            location,
            voterName,
            window.api.voteLocation.bind(window.api),
            window.api.unvoteLocation.bind(window.api)
        );
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
        alert('메뉴를 입력하세요');
        return;
    }
    try {
        const newMenu = await window.api.addMenu(schedule.scheduleId, name, url);
        menus.push(newMenu);
        addMenuToList(newMenu);
        nameInput.value = '';
        urlInput.value = '';
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
            <span class="location-votes">${menu.voteCount}표</span>
        </div>
        <div class="location-voters"></div>
        <div class="location-actions">
            <button class="btn btn-sm btn-primary menu-vote-btn"
                    data-action="menu.vote"
                    data-menu-id="${menu.id}">투표</button>
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

    try {
        await toggleVoteFor(
            menu,
            voterName,
            window.api.voteMenu.bind(window.api),
            window.api.unvoteMenu.bind(window.api)
        );
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
