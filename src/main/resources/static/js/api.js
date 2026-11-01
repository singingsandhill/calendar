// 오류 코드별 로케일 문구 — fragments/scripts.html 이 메시지 키 api.error.* 로 주입한다
const errorMessages = window.API_ERROR_MESSAGES;

const api = {
    async request(url, options = {}) {
        const defaultOptions = {
            headers: {
                'Content-Type': 'application/json'
            }
        };

        const response = await fetch(url, { ...defaultOptions, ...options });

        if (!response.ok) {
            const error = await response.json().catch(() => ({ code: 'UNKNOWN' }));
            const message = (error.code === 'VALIDATION_ERROR' && error.message)
                ? error.message
                : (errorMessages[error.code] || error.message || errorMessages.UNKNOWN);
            throw new Error(message);
        }

        if (response.status === 204) {
            return null;
        }

        return response.json();
    },

    // Owner API
    async getOwner(ownerId) {
        return this.request(`/api/owners/${ownerId}`);
    },

    async createOwner(ownerId) {
        return this.request('/api/owners', {
            method: 'POST',
            body: JSON.stringify({ ownerId })
        });
    },

    async getOwnerSchedules(ownerId) {
        return this.request(`/api/owners/${ownerId}/schedules`);
    },

    // Schedule API
    async getSchedule(ownerId, year, month) {
        return this.request(`/api/owners/${ownerId}/schedules/${year}/${month}`);
    },

    async createSchedule(ownerId, year, month, weeks = null) {
        const body = { year, month };
        if (weeks !== null) {
            body.weeks = weeks;
        }
        return this.request(`/api/owners/${ownerId}/schedules`, {
            method: 'POST',
            body: JSON.stringify(body)
        });
    },

    async deleteSchedule(ownerId, year, month) {
        return this.request(`/api/owners/${ownerId}/schedules/${year}/${month}`, {
            method: 'DELETE'
        });
    },

    async updateSchedule(ownerId, year, month, weeks) {
        return this.request(`/api/owners/${ownerId}/schedules/${year}/${month}`, {
            method: 'PATCH',
            body: JSON.stringify({ weeks })
        });
    },

    async updateScheduleDescription(ownerId, year, month, description) {
        return this.request(`/api/owners/${ownerId}/schedules/${year}/${month}`, {
            method: 'PATCH',
            body: JSON.stringify({ description })
        });
    },

    // Participant API
    async getParticipants(scheduleId) {
        return this.request(`/api/schedules/${scheduleId}/participants`);
    },

    async addParticipant(scheduleId, name) {
        return this.request(`/api/schedules/${scheduleId}/participants`, {
            method: 'POST',
            body: JSON.stringify({ name })
        });
    },

    async deleteParticipant(participantId) {
        return this.request(`/api/participants/${participantId}`, {
            method: 'DELETE'
        });
    },

    async updateSelections(participantId, selections) {
        return this.request(`/api/participants/${participantId}/selections`, {
            method: 'PATCH',
            body: JSON.stringify({ selections })
        });
    },

    // Location API
    async getLocations(scheduleId) {
        return this.request(`/api/schedules/${scheduleId}/locations`);
    },

    async addLocation(scheduleId, name) {
        return this.request(`/api/schedules/${scheduleId}/locations`, {
            method: 'POST',
            body: JSON.stringify({ name })
        });
    },

    async deleteLocation(locationId) {
        return this.request(`/api/locations/${locationId}`, {
            method: 'DELETE'
        });
    },

    async voteLocation(locationId, voterName) {
        return this.request(`/api/locations/${locationId}/votes`, {
            method: 'POST',
            body: JSON.stringify({ voterName })
        });
    },

    async unvoteLocation(locationId, voterName) {
        return this.request(`/api/locations/${locationId}/votes/${encodeURIComponent(voterName)}`, {
            method: 'DELETE'
        });
    },

    // Menu API
    async getMenus(scheduleId) {
        return this.request(`/api/schedules/${scheduleId}/menus`);
    },

    async addMenu(scheduleId, name, url = null) {
        return this.request(`/api/schedules/${scheduleId}/menus`, {
            method: 'POST',
            body: JSON.stringify({ name, url })
        });
    },

    async deleteMenu(menuId) {
        return this.request(`/api/menus/${menuId}`, {
            method: 'DELETE'
        });
    },

    async voteMenu(menuId, voterName) {
        return this.request(`/api/menus/${menuId}/votes`, {
            method: 'POST',
            body: JSON.stringify({ voterName })
        });
    },

    async unvoteMenu(menuId, voterName) {
        return this.request(`/api/menus/${menuId}/votes/${encodeURIComponent(voterName)}`, {
            method: 'DELETE'
        });
    },

    // Time Slot API
    async addTimeSlot(scheduleId, dayIndex, startMinute, endMinute) {
        return this.request(`/api/schedules/${scheduleId}/time-slots`, {
            method: 'POST',
            body: JSON.stringify({ dayIndex, startMinute, endMinute })
        });
    },

    async voteTimeSlot(timeSlotId, voterName) {
        return this.request(`/api/time-slots/${timeSlotId}/votes`, {
            method: 'POST',
            body: JSON.stringify({ voterName })
        });
    },

    async unvoteTimeSlot(timeSlotId, voterName) {
        return this.request(`/api/time-slots/${timeSlotId}/votes/${encodeURIComponent(voterName)}`, {
            method: 'DELETE'
        });
    }
};

window.api = api;
