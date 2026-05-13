package me.singingsandhill.calendar.datedate.infrastructure.security;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.singingsandhill.calendar.datedate.application.exception.InvalidOwnerIdException;
import me.singingsandhill.calendar.datedate.application.exception.OwnerNotFoundException;
import me.singingsandhill.calendar.datedate.application.exception.ReservedOwnerIdException;
import me.singingsandhill.calendar.datedate.domain.owner.OwnerRepository;
import me.singingsandhill.calendar.datedate.domain.owner.ReservedOwnerIds;

/**
 * API 쓰기 요청 경로의 {ownerId}를 사전 검증한다.
 *
 * 현재 인증 레이어가 없는 상태에서 최소한의 ID 변조/열거 방어를 제공한다.
 * Owner ID 형식은 도메인 규칙(^[a-z0-9-]+$, 2~20자)을 재사용한다.
 */
@Component
public class OwnerPathInterceptor implements HandlerInterceptor {

    private static final Pattern OWNER_PATH_PATTERN = Pattern.compile("^/api/owners/([^/]+)(?:/.*)?$");
    private static final Pattern OWNER_ID_PATTERN = Pattern.compile("^[a-z0-9-]+$");
    private static final int MIN_ID_LENGTH = 2;
    private static final int MAX_ID_LENGTH = 20;

    private final OwnerRepository ownerRepository;

    public OwnerPathInterceptor(OwnerRepository ownerRepository) {
        this.ownerRepository = ownerRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Matcher matcher = OWNER_PATH_PATTERN.matcher(request.getRequestURI());
        if (!matcher.matches()) {
            return true;
        }

        String ownerId = matcher.group(1);
        if (ownerId.length() < MIN_ID_LENGTH || ownerId.length() > MAX_ID_LENGTH
                || !OWNER_ID_PATTERN.matcher(ownerId).matches()) {
            throw new InvalidOwnerIdException("Owner ID format is invalid: " + ownerId);
        }
        if (ReservedOwnerIds.isReserved(ownerId)) {
            throw new ReservedOwnerIdException(ownerId);
        }

        if (isResourceMutation(request) && !ownerRepository.existsById(ownerId)) {
            throw new OwnerNotFoundException(ownerId);
        }
        return true;
    }

    /**
     * DELETE/PATCH/PUT은 기존 리소스를 대상으로 하므로 Owner 존재 검증을 수행한다.
     * POST는 getOrCreateOwner로 신규 생성 흐름을 허용한다.
     */
    private boolean isResourceMutation(HttpServletRequest request) {
        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        return method == HttpMethod.PUT
                || method == HttpMethod.PATCH
                || method == HttpMethod.DELETE;
    }
}
