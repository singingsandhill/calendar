package me.singingsandhill.calendar.datedate.domain.user;

import java.util.Optional;

public interface AppUserRepository {

    Optional<AppUser> findByKakaoId(Long kakaoId);

    Optional<AppUser> findById(Long id);

    AppUser save(AppUser user);
}
