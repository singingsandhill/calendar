const data = window.SCHEDULE_DATA;

export const schedule = {
    scheduleId: data.scheduleId,
    ownerId: data.ownerId,
    year: data.year,
    month: data.month,
    daysInMonth: data.daysInMonth,
    firstDayOfWeek: data.firstDayOfWeek,
    totalDays: data.totalDays,
    isExtendedMode: data.isExtendedMode
};

export const participants = data.participants;
export const locations = data.locations;
export const menus = data.menus;
export const messages = data.messages;

export function formatVotes(count) {
    return messages.votesTemplate.replace('{0}', count);
}

export const selection = {
    currentParticipantId: null,
    selectedDays: new Set()
};
