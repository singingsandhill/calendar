const api = {
    async request(url, options = {}) {
        const defaultOptions = {
            headers: {
                'Content-Type': 'application/json'
            }
        };

        const response = await fetch(url, { ...defaultOptions, ...options });

        if (!response.ok) {
            const error = await response.json().catch(() => ({ message: 'An error occurred' }));
            throw new Error(error.message || 'Request failed');
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
    }
};
