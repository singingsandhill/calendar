package me.singingsandhill.calendar.datedate.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import me.singingsandhill.calendar.datedate.domain.owner.Owner;
import me.singingsandhill.calendar.datedate.domain.owner.OwnerRepository;
import me.singingsandhill.calendar.datedate.domain.schedule.Schedule;
import me.singingsandhill.calendar.datedate.domain.schedule.ScheduleRepository;

@Service
@Transactional(readOnly = true)
public class OwnerService {

    private final OwnerRepository ownerRepository;
    private final ScheduleRepository scheduleRepository;

    public OwnerService(OwnerRepository ownerRepository, ScheduleRepository scheduleRepository) {
        this.ownerRepository = ownerRepository;
        this.scheduleRepository = scheduleRepository;
    }

    @Transactional
    public Owner getOrCreateOwner(String ownerId) {
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
}
