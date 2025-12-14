package me.singingsandhill.calendar.presentation.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import me.singingsandhill.calendar.application.exception.DuplicateScheduleException;
import me.singingsandhill.calendar.application.exception.ScheduleNotFoundException;
import me.singingsandhill.calendar.application.service.ScheduleService;
import me.singingsandhill.calendar.domain.schedule.Schedule;

@WebMvcTest(ScheduleApiController.class)
class ScheduleApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ScheduleService scheduleService;

    @Test
    @DisplayName("GET /api/owners/{ownerId}/schedules/{year}/{month} should return schedule")
    void getSchedule_existingSchedule_returnsSchedule() throws Exception {
        Schedule schedule = new Schedule("test-user", 2025, 12);
        schedule.setId(1L);
        when(scheduleService.getScheduleByOwnerAndYearMonth("test-user", 2025, 12))
                .thenReturn(schedule);

        mockMvc.perform(get("/api/owners/test-user/schedules/2025/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.ownerId").value("test-user"))
                .andExpect(jsonPath("$.year").value(2025))
                .andExpect(jsonPath("$.month").value(12));
    }

    @Test
    @DisplayName("GET /api/owners/{ownerId}/schedules/{year}/{month} should return 404 when not found")
    void getSchedule_notFound_returns404() throws Exception {
        when(scheduleService.getScheduleByOwnerAndYearMonth(anyString(), anyInt(), anyInt()))
                .thenThrow(new ScheduleNotFoundException("test-user", 2025, 12));

        mockMvc.perform(get("/api/owners/test-user/schedules/2025/12"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/owners/{ownerId}/schedules should create schedule")
    void createSchedule_validInput_createsSchedule() throws Exception {
        Schedule schedule = new Schedule("test-user", 2025, 12);
        schedule.setId(1L);
        when(scheduleService.createSchedule(anyString(), anyInt(), anyInt(), any()))
                .thenReturn(schedule);

        mockMvc.perform(post("/api/owners/test-user/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"year\": 2025, \"month\": 12}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.year").value(2025))
                .andExpect(jsonPath("$.month").value(12));
    }

    @Test
    @DisplayName("POST /api/owners/{ownerId}/schedules should return 409 for duplicate")
    void createSchedule_duplicate_returns409() throws Exception {
        when(scheduleService.createSchedule(anyString(), anyInt(), anyInt(), any()))
                .thenThrow(new DuplicateScheduleException("test-user", 2025, 12));

        mockMvc.perform(post("/api/owners/test-user/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"year\": 2025, \"month\": 12}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/owners/{ownerId}/schedules should return 400 for invalid input")
    void createSchedule_invalidInput_returns400() throws Exception {
        mockMvc.perform(post("/api/owners/test-user/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"year\": 2020, \"month\": 13}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/owners/{ownerId}/schedules/{year}/{month} should delete schedule")
    void deleteSchedule_existingSchedule_deletes() throws Exception {
        mockMvc.perform(delete("/api/owners/test-user/schedules/2025/12"))
                .andExpect(status().isNoContent());
    }
}
