package me.singingsandhill.calendar.datedate.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import me.singingsandhill.calendar.datedate.application.exception.OwnerIdTakenException;
import me.singingsandhill.calendar.datedate.application.exception.OwnerNotFoundException;
import me.singingsandhill.calendar.datedate.application.exception.ReservedOwnerIdException;
import me.singingsandhill.calendar.datedate.domain.owner.Owner;
import me.singingsandhill.calendar.datedate.domain.owner.OwnerIdGenerator;
import me.singingsandhill.calendar.datedate.domain.owner.OwnerRepository;
import me.singingsandhill.calendar.datedate.domain.owner.ReservedOwnerIds;
import me.singingsandhill.calendar.datedate.domain.schedule.Schedule;
import me.singingsandhill.calendar.datedate.domain.schedule.ScheduleRepository;

@Service
@Transactional(readOnly = true)
public class OwnerService {

    /**
     * 14,400,000 조합 중 연속으로 이만큼 충돌하려면 공간이 거의 소진돼야 한다.
     * 도달하면 조용히 중복 ID 를 내놓는 대신 실패시킨다 — 클라이언트가 재시도한다.
     */
    private static final int MAX_ID_GENERATION_ATTEMPTS = 10;

    private final OwnerRepository ownerRepository;
    private final ScheduleRepository scheduleRepository;
    private final OwnerIdGenerator ownerIdGenerator;

    public OwnerService(OwnerRepository ownerRepository,
                        ScheduleRepository scheduleRepository,
                        OwnerIdGenerator ownerIdGenerator) {
        this.ownerRepository = ownerRepository;
        this.scheduleRepository = scheduleRepository;
        this.ownerIdGenerator = ownerIdGenerator;
    }

    @Transactional
    public Owner getOrCreateOwner(String ownerId) {
        if (ReservedOwnerIds.isReserved(ownerId)) {
            throw new ReservedOwnerIdException(ownerId);
        }
        return ownerRepository.findById(ownerId)
                .orElseGet(() -> {
                    Owner newOwner = new Owner(ownerId);
                    return ownerRepository.save(newOwner);
                });
    }

    public Owner getOwner(String ownerId) {
        return ownerRepository.findById(ownerId).orElse(null);
    }

    public boolean ownerExists(String ownerId) {
        return ownerRepository.existsById(ownerId);
    }

    public List<Schedule> getOwnerSchedules(String ownerId) {
        return scheduleRepository.findAllByOwnerId(ownerId);
    }

    /**
     * 아직 아무도 쓰지 않는 owner ID 를 낸다 (홈 "랜덤 생성" 버튼).
     * 반환 시점의 미사용을 보장할 뿐이므로, 실제 점유는 {@link #createOwner} 가 다시 확인한다.
     */
    public String generateAvailableOwnerId() {
        for (int attempt = 0; attempt < MAX_ID_GENERATION_ATTEMPTS; attempt++) {
            String candidate = ownerIdGenerator.next();
            if (!ownerRepository.existsById(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException(
                "Could not find an unused owner ID in " + MAX_ID_GENERATION_ATTEMPTS + " attempts");
    }

    /**
     * 새 owner 를 만든다 — 이미 있으면 {@link OwnerIdTakenException}.
     * 재진입이 정상인 직접 입력 경로({@link #getOrCreateOwner})와 달리, 랜덤 생성으로 받은 ID 는
     * 이미 존재한다면 그 자체가 충돌이므로 남의 페이지로 흘려보내지 않고 실패시킨다.
     */
    @Transactional
    public Owner createOwner(String ownerId, Long userId) {
        if (ReservedOwnerIds.isReserved(ownerId)) {
            throw new ReservedOwnerIdException(ownerId);
        }
        if (ownerRepository.existsById(ownerId)) {
            throw new OwnerIdTakenException(ownerId);
        }
        Owner owner = new Owner(ownerId);
        if (userId != null) {
            owner.linkUser(userId);
        }
        return ownerRepository.save(owner);
    }

    @Transactional
    public Owner getOrCreateOwner(String ownerId, Long userId) {
        Owner owner = getOrCreateOwner(ownerId);
        if (userId != null && owner.getUserId() == null) {
            owner.linkUser(userId);
            owner = ownerRepository.save(owner);
        }
        return owner;
    }

    @Transactional
    public Owner linkOwnerToUser(String ownerId, Long userId) {
        Owner owner = ownerRepository.findById(ownerId)
                .orElseThrow(() -> new OwnerNotFoundException(ownerId));
        owner.linkUser(userId);
        return ownerRepository.save(owner);
    }

    public List<Owner> getOwnersOf(Long userId) {
        return ownerRepository.findAllByUserId(userId);
    }
}
