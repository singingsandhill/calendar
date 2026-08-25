package me.singingsandhill.calendar.datedate.application.exception;

import org.springframework.http.HttpStatus;

import me.singingsandhill.calendar.common.application.exception.BusinessException;

/**
 * 새로 만들려던 owner ID 가 이미 점유돼 있을 때. 사용자가 직접 입력한 ID 는 재진입
 * (get-or-create) 이 정상이므로 이 예외는 <em>랜덤 생성 경로</em>에서만 발생한다.
 */
public class OwnerIdTakenException extends BusinessException {

    public OwnerIdTakenException(String ownerId) {
        super("OWNER_ID_TAKEN",
                "This ID is taken or reserved: " + ownerId,
                HttpStatus.CONFLICT,
                ReservedOwnerIdException.MESSAGE_KEY,
                new Object[]{ownerId});
    }
}
