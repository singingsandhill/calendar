package me.singingsandhill.calendar.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import me.singingsandhill.calendar.application.exception.DuplicateLocationException;
import me.singingsandhill.calendar.application.exception.LocationNotFoundException;
import me.singingsandhill.calendar.application.exception.ScheduleNotFoundException;
import me.singingsandhill.calendar.domain.location.Location;
import me.singingsandhill.calendar.domain.location.LocationRepository;
import me.singingsandhill.calendar.domain.schedule.ScheduleRepository;

@Service
@Transactional(readOnly = true)
public class LocationService {

    private final LocationRepository locationRepository;
    private final ScheduleRepository scheduleRepository;

    public LocationService(LocationRepository locationRepository,
                           ScheduleRepository scheduleRepository) {
        this.locationRepository = locationRepository;
        this.scheduleRepository = scheduleRepository;
    }

    public Location getLocation(Long locationId) {
        return locationRepository.findById(locationId)
                .orElseThrow(() -> new LocationNotFoundException(locationId));
    }

    public List<Location> getLocationsByScheduleId(Long scheduleId) {
        return locationRepository.findAllByScheduleId(scheduleId);
    }

    @Transactional
    public Location addLocation(Long scheduleId, String name) {
        scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ScheduleNotFoundException(scheduleId));

        if (locationRepository.existsByScheduleIdAndName(scheduleId, name)) {
            throw new DuplicateLocationException(name);
        }

        Location location = new Location(scheduleId, name);
        return locationRepository.save(location);
    }

    @Transactional
    public void deleteLocation(Long locationId) {
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new LocationNotFoundException(locationId));
        locationRepository.delete(location);
    }

    @Transactional
    public Location vote(Long locationId, String voterName) {
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new LocationNotFoundException(locationId));

        location.addVote(voterName);
        return locationRepository.save(location);
    }

    @Transactional
    public Location unvote(Long locationId, String voterName) {
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new LocationNotFoundException(locationId));

        location.removeVote(voterName);
        return locationRepository.save(location);
    }
}
