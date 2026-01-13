/**
 * run-detail.js
 * Runner run detail page functionality
 *
 * Requires: window.RUN_DATA to be defined before this script loads
 * - runId
 */

(function() {
    'use strict';

    const { runId } = window.RUN_DATA;

    // Delete modal state (admin only)
    let deleteAttendanceId = null;

    document.addEventListener('DOMContentLoaded', function() {
        setupAttendanceForm();
        setupDeleteModal();
    });

    // ==================== Attendance Form ====================

    function setupAttendanceForm() {
        const form = document.getElementById('attendanceForm');
        if (form) {
            form.addEventListener('submit', handleAttendanceSubmit);
        }
    }

    async function handleAttendanceSubmit(e) {
        e.preventDefault();

        const participantName = document.getElementById('participantName').value.trim();
        const distance = parseFloat(document.getElementById('distance').value);

        if (!participantName || !distance) {
            showMessage('모든 필드를 입력해주세요.', 'error');
            return;
        }

        try {
            const response = await fetch(`/runners/runs/${runId}/attendance`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    participantName: participantName,
                    distance: distance
                })
            });

            if (response.ok) {
                const data = await response.json();
                showMessage('출석이 등록되었습니다!', 'success');
                addAttendanceToList(data.participantName, data.distance);
                document.getElementById('attendanceForm').reset();
            } else {
                const error = await response.json();
                if (error.code === 'DUPLICATE_ATTENDANCE') {
                    showMessage('이미 출석 등록된 이름입니다.', 'error');
                } else {
                    showMessage(error.message || '출석 등록에 실패했습니다.', 'error');
                }
            }
        } catch (error) {
            showMessage('네트워크 오류가 발생했습니다.', 'error');
        }
    }

    function showMessage(message, type) {
        const messageDiv = document.getElementById('formMessage');
        messageDiv.textContent = message;
        messageDiv.className = 'runners-message ' + (type === 'success' ? 'runners-message-success' : 'runners-message-error');
        messageDiv.style.display = 'block';

        setTimeout(() => {
            messageDiv.style.display = 'none';
        }, 3000);
    }

    function addAttendanceToList(name, distance) {
        const list = document.getElementById('attendanceList');
        const emptyState = document.getElementById('emptyState');
        const countSpan = document.getElementById('attendanceCount');

        if (emptyState) {
            emptyState.style.display = 'none';
        }

        const li = document.createElement('li');
        li.className = 'runners-attendance-item';
        li.innerHTML = `
            <span class="runners-attendance-name">${escapeHtml(name)}</span>
            <span class="runners-attendance-distance">${distance}km</span>
        `;
        list.appendChild(li);

        const currentCount = parseInt(countSpan.textContent) || 0;
        countSpan.textContent = currentCount + 1;
    }

    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    // ==================== Delete Modal (Admin Only) ====================

    function setupDeleteModal() {
        const deleteModal = document.getElementById('deleteModal');
        const confirmDeleteBtn = document.getElementById('confirmDelete');
        const cancelDeleteBtn = document.getElementById('cancelDelete');

        if (!deleteModal) return; // Not admin

        // Attach click event to delete buttons
        document.querySelectorAll('.runners-delete-btn').forEach(btn => {
            btn.addEventListener('click', function() {
                deleteAttendanceId = this.dataset.id;
                document.getElementById('deleteName').textContent = this.dataset.name;
                deleteModal.classList.add('show');
            });
        });

        // Cancel delete
        if (cancelDeleteBtn) {
            cancelDeleteBtn.addEventListener('click', function() {
                deleteModal.classList.remove('show');
                deleteAttendanceId = null;
            });
        }

        // Confirm delete
        if (confirmDeleteBtn) {
            confirmDeleteBtn.addEventListener('click', handleConfirmDelete);
        }

        // Close modal on backdrop click
        deleteModal.addEventListener('click', function(e) {
            if (e.target === deleteModal) {
                deleteModal.classList.remove('show');
                deleteAttendanceId = null;
            }
        });
    }

    async function handleConfirmDelete() {
        if (!deleteAttendanceId) return;

        const deleteModal = document.getElementById('deleteModal');

        try {
            const response = await fetch(`/runners/admin/attendance/${deleteAttendanceId}/delete`, {
                method: 'POST'
            });

            if (response.ok) {
                // Remove the item from DOM
                const item = document.querySelector(`li[data-id="${deleteAttendanceId}"]`);
                if (item) {
                    item.remove();
                }

                // Update count
                const countSpan = document.getElementById('attendanceCount');
                const currentCount = parseInt(countSpan.textContent) || 0;
                countSpan.textContent = Math.max(0, currentCount - 1);

                // Show empty state if no attendances left
                const list = document.getElementById('attendanceList');
                if (list.children.length === 0) {
                    const emptyState = document.getElementById('emptyState');
                    if (emptyState) {
                        emptyState.style.display = 'block';
                    }
                }

                showMessage('출석 기록이 삭제되었습니다.', 'success');
            } else {
                showMessage('삭제에 실패했습니다.', 'error');
            }
        } catch (error) {
            showMessage('네트워크 오류가 발생했습니다.', 'error');
        } finally {
            deleteModal.classList.remove('show');
            deleteAttendanceId = null;
        }
    }

})();
