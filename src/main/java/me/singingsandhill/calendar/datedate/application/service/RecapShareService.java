package me.singingsandhill.calendar.datedate.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import me.singingsandhill.calendar.datedate.application.exception.InvalidRecapYearException;
import me.singingsandhill.calendar.datedate.application.exception.RecapShareNotFoundException;
import me.singingsandhill.calendar.datedate.domain.recap.RecapShare;
import me.singingsandhill.calendar.datedate.domain.recap.RecapShareRepository;

@Service
@Transactional(readOnly = true)
public class RecapShareService {

    private static final int MIN_YEAR = 2024;

    private final RecapShareRepository recapShareRepository;
    private final Clock clock;

    public RecapShareService(RecapShareRepository recapShareRepository, Clock clock) {
        this.recapShareRepository = recapShareRepository;
        this.clock = clock;
    }

    @Transactional
    public RecapShare getOrCreateShare(Long userId, int year) {
        // 직접 API 호출로 렌더링 불가능한 연도의 고아 공유 토큰이 생기는 것을 차단
        // (RecapService.validateYear 와 동일 범위).
        if (year < MIN_YEAR || year > Year.now(clock).getValue()) {
            throw new InvalidRecapYearException(year);
        }
        return recapShareRepository.findByUserIdAndYear(userId, year)
                .orElseGet(() -> recapShareRepository.save(new RecapShare(
                        null, userId, year, UUID.randomUUID().toString(), LocalDateTime.now(clock))));
    }

    public RecapShare getByToken(String token) {
        return recapShareRepository.findByToken(token)
                .orElseThrow(() -> new RecapShareNotFoundException(token));
    }
}
