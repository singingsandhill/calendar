// Calendar utility functions

/**
 * Get the number of days in a month
 */
function getDaysInMonth(year, month) {
    return new Date(year, month, 0).getDate();
}

/**
 * Get the day of the week for the first day of a month (0 = Sunday)
 */
function getFirstDayOfWeek(year, month) {
    return new Date(year, month - 1, 1).getDay();
}

/**
 * Format a date for display
 */
function formatDate(year, month, day) {
    return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
}

/**
 * Format month for display in Korean
 */
function formatMonthKorean(year, month) {
    return `${year}년 ${month}월`;
}

/**
 * Get month name in Korean
 */
function getMonthNameKorean(month) {
    const months = ['', '1월', '2월', '3월', '4월', '5월', '6월',
                    '7월', '8월', '9월', '10월', '11월', '12월'];
    return months[month];
}

/**
 * Calculate the number of weeks needed to display a month
 */
function calculateWeeks(year, month) {
    const daysInMonth = getDaysInMonth(year, month);
    const firstDayOfWeek = getFirstDayOfWeek(year, month);
    return Math.ceil((daysInMonth + firstDayOfWeek) / 7);
}

/**
 * Check if a date is today
 */
function isToday(year, month, day) {
    const today = new Date();
    return today.getFullYear() === year &&
           today.getMonth() + 1 === month &&
           today.getDate() === day;
}

/**
 * Check if a date is in the past
 */
function isPastDate(year, month, day) {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const date = new Date(year, month - 1, day);
    return date < today;
}
