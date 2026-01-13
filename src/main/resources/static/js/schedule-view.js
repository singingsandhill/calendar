/**
 * schedule-view.js
 * Schedule calendar view functionality
 *
 * Requires: window.SCHEDULE_DATA to be defined before this script loads
 * - scheduleId, ownerId, year, month, daysInMonth, firstDayOfWeek, totalDays, isExtendedMode
 * - participants, locations, menus arrays
 */

(function() {
    'use strict';

    // Get data from global scope (injected by Thymeleaf)
    const {
        scheduleId,
        ownerId,
        year,
        month,
        daysInMonth,
        firstDayOfWeek,
        totalDays,
        isExtendedMode,
        participants,
        locations,
        menus
    } = window.SCHEDULE_DATA;

    let currentParticipantId = null;
    let selectedDays = new Set();

    // Initialize on DOM ready
    document.addEventListener('DOMContentLoaded', function() {
        renderCalendar();
        setupEventListeners();
    });

    function setupEventListeners() {
        // Add participant form
        const addParticipantForm = document.getElementById('addParticipantForm');
        if (addParticipantForm) {
            addParticipantForm.addEventListener('submit', handleAddParticipant);
        }

        // Location input enter key
        const locationInput = document.getElementById('locationInput');
        if (locationInput) {
            locationInput.addEventListener('keypress', function(e) {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    addLocation();
                }
            });
        }

        // Menu input enter key
        const menuInput = document.getElementById('menuInput');
        if (menuInput) {
            menuInput.addEventListener('keypress', function(e) {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    addMenu();
                }
            });
        }
    }

    // ==================== Calendar Rendering ====================

    function renderCalendar() {
        const calendarBody = document.getElementById('calendarBody');
        calendarBody.innerHTML = '';

        if (isExtendedMode) {
            renderExtendedCalendar(calendarBody);
        } else {
            renderLegacyCalendar(calendarBody);
        }
    }

    function renderExtendedCalendar(calendarBody) {
        const firstOfMonth = new Date(year, month - 1, 1);
        const gridStart = new Date(firstOfMonth);
        gridStart.setDate(gridStart.getDate() - firstDayOfWeek);

        for (let index = 1; index <= totalDays; index++) {
            const currentDate = new Date(gridStart);
            currentDate.setDate(gridStart.getDate() + (index - 1));

            const dayCell = document.createElement('div');
            dayCell.className = 'calendar-day';
            dayCell.dataset.day = index;

            const dayNumber = document.createElement('span');
            dayNumber.className = 'day-number';

            const displayDay = currentDate.getDate();
            const displayMonth = currentDate.getMonth() + 1;

            if (displayMonth !== month) {
                dayCell.classList.add('other-month');
                dayNumber.textContent = `${displayMonth}/${displayDay}`;
            } else {
                dayNumber.textContent = displayDay;
            }

            dayCell.appendChild(dayNumber);

            const dotsContainer = document.createElement('div');
            dotsContainer.className = 'participant-dots';

            participants.forEach(p => {
                if (p.selections && p.selections.includes(index)) {
                    const dot = document.createElement('span');
                    dot.className = 'participant-dot';
                    dot.style.backgroundColor = p.color;
                    dot.title = p.name;
                    dotsContainer.appendChild(dot);
                }
            });

            dayCell.appendChild(dotsContainer);
            dayCell.addEventListener('click', () => toggleDay(index, dayCell));
            calendarBody.appendChild(dayCell);
        }
    }

    function renderLegacyCalendar(calendarBody) {
        for (let i = 0; i < firstDayOfWeek; i++) {
            const emptyCell = document.createElement('div');
            emptyCell.className = 'calendar-day empty';
            calendarBody.appendChild(emptyCell);
        }

        for (let day = 1; day <= daysInMonth; day++) {
            const dayCell = document.createElement('div');
            dayCell.className = 'calendar-day';
            dayCell.dataset.day = day;

            const dayNumber = document.createElement('span');
            dayNumber.className = 'day-number';
            dayNumber.textContent = day;
            dayCell.appendChild(dayNumber);

            const dotsContainer = document.createElement('div');
            dotsContainer.className = 'participant-dots';

            participants.forEach(p => {
                if (p.selections && p.selections.includes(day)) {
                    const dot = document.createElement('span');
                    dot.className = 'participant-dot';
                    dot.style.backgroundColor = p.color;
                    dot.title = p.name;
                    dotsContainer.appendChild(dot);
                }
            });

            dayCell.appendChild(dotsContainer);
            dayCell.addEventListener('click', () => toggleDay(day, dayCell));
            calendarBody.appendChild(dayCell);
        }
    }

    // ==================== Participant Selection ====================

    window.onParticipantChange = function() {
        const select = document.getElementById('participantSelect');
        currentParticipantId = select.value ? parseInt(select.value) : null;

        selectedDays.clear();
        if (currentParticipantId) {
            const participant = participants.find(p => p.id === currentParticipantId);
            if (participant && participant.selections) {
                participant.selections.forEach(day => selectedDays.add(day));
            }
        }

        updateCalendarDisplay();
    };

    function updateCalendarDisplay() {
        document.querySelectorAll('.calendar-day').forEach(cell => {
            cell.classList.remove('selected');
            const day = parseInt(cell.dataset.day);
            if (selectedDays.has(day)) {
                cell.classList.add('selected');
            }
        });
    }

    function toggleDay(day, cell) {
        if (!currentParticipantId) {
            alert('먼저 이름을 선택하세요');
            return;
        }

        if (selectedDays.has(day)) {
            selectedDays.delete(day);
            cell.classList.remove('selected');
        } else {
            selectedDays.add(day);
            cell.classList.add('selected');
        }
    }

    window.resetSelections = function() {
        if (!currentParticipantId) {
            alert('먼저 이름을 선택하세요');
            return;
        }
        selectedDays.clear();
        updateCalendarDisplay();
    };

    window.saveSelections = async function() {
        if (!currentParticipantId) {
            alert('먼저 이름을 선택하세요');
            return;
        }

        try {
            await api.updateSelections(currentParticipantId, Array.from(selectedDays));
            const participant = participants.find(p => p.id === currentParticipantId);
            if (participant) {
                participant.selections = Array.from(selectedDays);
            }
            renderCalendar();
            updateCalendarDisplay();
            toast.success('저장되었습니다!');
        } catch (error) {
            toast.error(error.message);
        }
    };

    // ==================== Participant Modal ====================

    window.openAddParticipantModal = function() {
        document.getElementById('addParticipantModal').classList.add('show');
    };

    window.closeAddParticipantModal = function() {
        document.getElementById('addParticipantModal').classList.remove('show');
    };

    async function handleAddParticipant(e) {
        e.preventDefault();
        const name = document.getElementById('participantName').value.trim();

        try {
            await api.addParticipant(scheduleId, name);
            window.location.reload();
        } catch (error) {
            toast.error(error.message);
        }
    }

    // ==================== Copy Link ====================

    window.copyLink = function() {
        const url = window.location.href;

        if (navigator.clipboard && window.isSecureContext) {
            navigator.clipboard.writeText(url)
                .then(() => alert('링크가 클립보드에 복사되었습니다!'))
                .catch(() => fallbackCopy(url));
        } else {
            fallbackCopy(url);
        }
    };

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

    // ==================== Location Voting ====================

    window.addLocation = async function() {
        const input = document.getElementById('locationInput');
        const name = input.value.trim();

        if (!name) {
            alert('장소를 입력하세요');
            return;
        }

        try {
            const newLocation = await api.addLocation(scheduleId, name);
            locations.push(newLocation);
            addLocationToList(newLocation);
            input.value = '';
        } catch (error) {
            toast.error(error.message);
        }
    };

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
                        data-location-id="${location.id}"
                        onclick="toggleVote(this)">투표</button>
            </div>
        `;
        list.appendChild(item);
    }

    window.toggleVote = async function(button) {
        const locationId = parseInt(button.dataset.locationId);
        const select = document.getElementById('participantSelect');
        const selectedOption = select.options[select.selectedIndex];

        if (!select.value) {
            alert('먼저 이름을 선택하세요');
            return;
        }

        const voterName = selectedOption.text;
        const location = locations.find(l => l.id === locationId);
        const hasVoted = location && location.voters.some(v => v.toLowerCase() === voterName.toLowerCase());

        try {
            if (hasVoted) {
                await api.unvoteLocation(locationId, voterName);
                location.voters = location.voters.filter(v => v.toLowerCase() !== voterName.toLowerCase());
                location.voteCount--;
            } else {
                await api.voteLocation(locationId, voterName);
                location.voters.push(voterName);
                location.voteCount++;
            }
            updateLocationItemUI(button.closest('.location-item'), location);
        } catch (error) {
            toast.error(error.message);
        }
    };

    function updateLocationItemUI(itemElement, location) {
        const votesSpan = itemElement.querySelector('.location-votes');
        votesSpan.textContent = location.voteCount + '표';

        const votersDiv = itemElement.querySelector('.location-voters');
        votersDiv.innerHTML = location.voters.map(v =>
            `<span class="voter-tag">${escapeHtml(v)}</span>`
        ).join('');
    }

    // ==================== Menu Voting ====================

    window.addMenu = async function() {
        const nameInput = document.getElementById('menuInput');
        const urlInput = document.getElementById('menuUrlInput');
        const name = nameInput.value.trim();
        const url = urlInput.value.trim() || null;

        if (!name) {
            alert('메뉴를 입력하세요');
            return;
        }

        try {
            const newMenu = await api.addMenu(scheduleId, name, url);
            menus.push(newMenu);
            addMenuToList(newMenu);
            nameInput.value = '';
            urlInput.value = '';
        } catch (error) {
            toast.error(error.message);
        }
    };

    function addMenuToList(menu) {
        const list = document.getElementById('menuList');
        const emptyMsg = list.querySelector('.empty-locations');
        if (emptyMsg) emptyMsg.remove();

        const item = document.createElement('div');
        item.className = 'location-item';
        const urlLink = menu.url ? `<a href="${escapeHtml(menu.url)}" target="_blank" rel="noopener noreferrer" class="menu-link">🔗</a>` : '';
        item.innerHTML = `
            <div class="location-info">
                <span class="location-name">${escapeHtml(menu.name)}</span>
                ${urlLink}
                <span class="location-votes">${menu.voteCount}표</span>
            </div>
            <div class="location-voters"></div>
            <div class="location-actions">
                <button class="btn btn-sm btn-primary menu-vote-btn"
                        data-menu-id="${menu.id}"
                        onclick="toggleMenuVote(this)">투표</button>
            </div>
        `;
        list.appendChild(item);
    }

    window.toggleMenuVote = async function(button) {
        const menuId = parseInt(button.dataset.menuId);
        const select = document.getElementById('participantSelect');
        const selectedOption = select.options[select.selectedIndex];

        if (!select.value) {
            alert('먼저 이름을 선택하세요');
            return;
        }

        const voterName = selectedOption.text;
        const menu = menus.find(m => m.id === menuId);
        const hasVoted = menu && menu.voters.some(v => v.toLowerCase() === voterName.toLowerCase());

        try {
            if (hasVoted) {
                await api.unvoteMenu(menuId, voterName);
                menu.voters = menu.voters.filter(v => v.toLowerCase() !== voterName.toLowerCase());
                menu.voteCount--;
            } else {
                await api.voteMenu(menuId, voterName);
                menu.voters.push(voterName);
                menu.voteCount++;
            }
            updateMenuItemUI(button.closest('.location-item'), menu);
        } catch (error) {
            toast.error(error.message);
        }
    };

    function updateMenuItemUI(itemElement, menu) {
        const votesSpan = itemElement.querySelector('.location-votes');
        votesSpan.textContent = menu.voteCount + '표';

        const votersDiv = itemElement.querySelector('.location-voters');
        votersDiv.innerHTML = menu.voters.map(v =>
            `<span class="voter-tag">${escapeHtml(v)}</span>`
        ).join('');
    }

    // ==================== Utilities ====================

    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

})();
