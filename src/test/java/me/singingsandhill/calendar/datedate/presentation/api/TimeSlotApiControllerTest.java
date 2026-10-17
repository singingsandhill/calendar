package me.singingsandhill.calendar.datedate.presentation.api;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import me.singingsandhill.calendar.datedate.application.exception.DuplicateTimeSlotException;
import me.singingsandhill.calendar.datedate.application.exception.TimeSlotNotFoundException;
import me.singingsandhill.calendar.datedate.application.service.TimeSlotService;
import me.singingsandhill.calendar.datedate.domain.owner.OwnerRepository;
import me.singingsandhill.calendar.datedate.domain.timeslot.TimeSlot;
import me.singingsandhill.calendar.runner.domain.AdminRepository;

@WebMvcTest(TimeSlotApiController.class)
@WithMockUser
class TimeSlotApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TimeSlotService timeSlotService;

    @MockitoBean
    private AdminRepository adminRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private OwnerRepository ownerRepository;

    @Test
    @DisplayName("POST /api/schedules/{id}/time-slots 는 201 과 후보 JSON 을 반환한다")
    void addTimeSlot_valid_returns201() throws Exception {
        when(timeSlotService.addTimeSlot(1L, 20, 1080, 1200))
                .thenReturn(new TimeSlot(7L, 1L, 20, 1080, 1200, List.of(), LocalDateTime.now()));

        mockMvc.perform(post("/api/schedules/1/time-slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dayIndex\": 20, \"startMinute\": 1080, \"endMinute\": 1200}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.dayIndex").value(20))
                .andExpect(jsonPath("$.startMinute").value(1080))
                .andExpect(jsonPath("$.endMinute").value(1200))
                .andExpect(jsonPath("$.voteCount").value(0))
                .andExpect(jsonPath("$.voters").isEmpty());
    }

    @Test
    @DisplayName("POST time-slots: 날짜 누락·24:00 초과 요청은 400 이고 서비스에 도달하지 않는다")
    void addTimeSlot_invalidBody_returns400() throws Exception {
        mockMvc.perform(post("/api/schedules/1/time-slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startMinute\": 1080, \"endMinute\": 1200}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/schedules/1/time-slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dayIndex\": 20, \"startMinute\": 1410, \"endMinute\": 1470}"))
                .andExpect(status().isBadRequest());

        verify(timeSlotService, never()).addTimeSlot(anyLong(), anyInt(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("POST time-slots: 중복 후보는 409")
    void addTimeSlot_duplicate_returns409() throws Exception {
        when(timeSlotService.addTimeSlot(1L, 20, 1080, 1200))
                .thenThrow(new DuplicateTimeSlotException(20, 1080, 1200));

        mockMvc.perform(post("/api/schedules/1/time-slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dayIndex\": 20, \"startMinute\": 1080, \"endMinute\": 1200}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_TIME_SLOT"));
    }

    @Test
    @DisplayName("POST /api/time-slots/{id}/votes 는 갱신된 투표자 목록을 반환한다")
    void vote_returnsUpdatedVoters() throws Exception {
        when(timeSlotService.vote(7L, "Alice"))
                .thenReturn(new TimeSlot(7L, 1L, 20, 1080, 1200, List.of("Alice"), LocalDateTime.now()));

        mockMvc.perform(post("/api/time-slots/7/votes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"voterName\": \"Alice\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voters[0]").value("Alice"))
                .andExpect(jsonPath("$.voteCount").value(1));
    }

    @Test
    @DisplayName("DELETE /api/time-slots/{id}/votes/{voterName} 는 투표를 취소한다")
    void unvote_returnsUpdatedVoters() throws Exception {
        when(timeSlotService.unvote(7L, "Alice"))
                .thenReturn(new TimeSlot(7L, 1L, 20, 1080, 1200, List.of(), LocalDateTime.now()));

        mockMvc.perform(delete("/api/time-slots/7/votes/Alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voteCount").value(0));
    }

    @Test
    @DisplayName("없는 후보에 투표하면 404")
    void vote_missingSlot_returns404() throws Exception {
        when(timeSlotService.vote(anyLong(), anyString()))
                .thenThrow(new TimeSlotNotFoundException(99L));

        mockMvc.perform(post("/api/time-slots/99/votes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"voterName\": \"Alice\"}"))
                .andExpect(status().isNotFound());
    }
}
