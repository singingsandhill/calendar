package me.singingsandhill.calendar.datedate.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import me.singingsandhill.calendar.datedate.application.exception.UserNotFoundException;
import me.singingsandhill.calendar.datedate.domain.user.AppUser;
import me.singingsandhill.calendar.datedate.domain.user.AppUserRepository;

@Service
@Transactional(readOnly = true)
public class AppUserService {

    private final AppUserRepository appUserRepository;
    private final Clock clock;

    public AppUserService(AppUserRepository appUserRepository, Clock clock) {
        this.appUserRepository = appUserRepository;
        this.clock = clock;
    }

    @Transactional
    public AppUser upsertKakaoUser(Long kakaoId, String nickname, String profileImageUrl) {
        LocalDateTime now = LocalDateTime.now(clock);
        return appUserRepository.findByKakaoId(kakaoId)
                .map(user -> {
                    user.refreshProfile(nickname, profileImageUrl, now);
                    return appUserRepository.save(user);
                })
                .orElseGet(() -> appUserRepository.save(
                        AppUser.signUp(kakaoId, nickname, profileImageUrl, now)));
    }

    public AppUser getUser(Long userId) {
        return appUserRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }
}
