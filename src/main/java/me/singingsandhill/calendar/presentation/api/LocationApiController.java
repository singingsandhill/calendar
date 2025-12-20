package me.singingsandhill.calendar.presentation.api;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import me.singingsandhill.calendar.application.service.LocationService;
import me.singingsandhill.calendar.domain.location.Location;
import me.singingsandhill.calendar.presentation.dto.request.LocationCreateRequest;
import me.singingsandhill.calendar.presentation.dto.request.VoteRequest;
import me.singingsandhill.calendar.presentation.dto.response.LocationResponse;

@RestController
@RequestMapping("/api")
public class LocationApiController {

    private final LocationService locationService;

    public LocationApiController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping("/schedules/{scheduleId}/locations")
    public ResponseEntity<List<LocationResponse>> getLocations(@PathVariable Long scheduleId) {
        List<Location> locations = locationService.getLocationsByScheduleId(scheduleId);
        List<LocationResponse> responses = locations.stream()
                .map(LocationResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/schedules/{scheduleId}/locations")
    public ResponseEntity<LocationResponse> addLocation(
            @PathVariable Long scheduleId,
            @Valid @RequestBody LocationCreateRequest request) {
        Location location = locationService.addLocation(scheduleId, request.name());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(LocationResponse.from(location));
    }

    @DeleteMapping("/locations/{locationId}")
    public ResponseEntity<Void> deleteLocation(@PathVariable Long locationId) {
        locationService.deleteLocation(locationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/locations/{locationId}/votes")
    public ResponseEntity<LocationResponse> vote(
            @PathVariable Long locationId,
            @Valid @RequestBody VoteRequest request) {
        Location location = locationService.vote(locationId, request.voterName());
        return ResponseEntity.ok(LocationResponse.from(location));
    }

    @DeleteMapping("/locations/{locationId}/votes/{voterName}")
    public ResponseEntity<LocationResponse> unvote(
            @PathVariable Long locationId,
            @PathVariable String voterName) {
        Location location = locationService.unvote(locationId, voterName);
        return ResponseEntity.ok(LocationResponse.from(location));
    }
}
