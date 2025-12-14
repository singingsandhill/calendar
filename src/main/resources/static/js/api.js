const errorMessages = {
    'VALIDATION_ERROR': '입력값이 올바르지 않습니다',
    'INVALID_ARGUMENT': '잘못된 요청입니다',
    'INVALID_OWNER_ID': '사용자 ID 형식이 올바르지 않습니다',
    'OWNER_NOT_FOUND': '사용자를 찾을 수 없습니다',
    'SCHEDULE_NOT_FOUND': '일정을 찾을 수 없습니다',
    'DUPLICATE_SCHEDULE': '이미 존재하는 일정입니다',
    'PARTICIPANT_NOT_FOUND': '참여자를 찾을 수 없습니다',
    'MAX_PARTICIPANTS_EXCEEDED': '참여자는 최대 8명까지 가능합니다',
    'DUPLICATE_PARTICIPANT': '이미 같은 이름의 참여자가 있습니다',
    'INTERNAL_ERROR': '서버 오류가 발생했습니다'
};

const api = {
    async request(url, options = {}) {
        const defaultOptions = {
            headers: {
                'Content-Type': 'application/json'
            }
        };

        const response = await fetch(url, { ...defaultOptions, ...options });

        if (!response.ok) {
            const error = await response.json().catch(() => ({ code: 'UNKNOWN', message: '오류가 발생했습니다' }));
            const koreanMessage = errorMessages[error.code] || error.message || '오류가 발생했습니다';
            throw new Error(koreanMessage);
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
