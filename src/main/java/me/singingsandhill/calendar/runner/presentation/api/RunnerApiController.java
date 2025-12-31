package me.singingsandhill.calendar.runner.presentation.api;

import jakarta.validation.Valid;
import me.singingsandhill.calendar.runner.application.service.AttendanceService;
import me.singingsandhill.calendar.runner.domain.Attendance;
import me.singingsandhill.calendar.runner.presentation.dto.request.AttendanceCreateRequest;
import me.singingsandhill.calendar.runner.presentation.dto.response.AttendanceResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/runners/runs")
public class RunnerApiController {

    private final AttendanceService attendanceService;

    public RunnerApiController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping("/{runId}/attendance")
    public ResponseEntity<AttendanceResponse> registerAttendance(
            @PathVariable Long runId,
            @Valid @RequestBody AttendanceCreateRequest request) {

        Attendance attendance = attendanceService.registerAttendance(
            runId,
            request.participantName(),
            request.distance()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AttendanceResponse.from(attendance));
    }
}
