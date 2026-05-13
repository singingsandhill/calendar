package me.singingsandhill.calendar.datedate.infrastructure.migration;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import me.singingsandhill.calendar.datedate.domain.owner.OwnerRepository;
import me.singingsandhill.calendar.datedate.domain.owner.ReservedOwnerIds;

/**
 * 시작 시 예약어를 ID로 점유한 기존 owner를 탐지해 경고 로그를 남긴다.
 * 충돌이 발견되면 별도 마이그레이션(리네이밍/삭제)이 필요하다.
 */
@Component
public class ReservedOwnerIdMigrationCheck implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ReservedOwnerIdMigrationCheck.class);

    private final OwnerRepository ownerRepository;

    public ReservedOwnerIdMigrationCheck(OwnerRepository ownerRepository) {
        this.ownerRepository = ownerRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> conflicts = ReservedOwnerIds.RESERVED.stream()
                .filter(ownerRepository::existsById)
                .sorted()
                .toList();

        if (conflicts.isEmpty()) {
            log.info("RESERVED_ID_CHECK ok=true conflicts=0");
            return;
        }
        log.warn("RESERVED_ID_CHECK ok=false conflicts={} ids={} action=manual_migration_required",
                conflicts.size(), conflicts);
    }
}
