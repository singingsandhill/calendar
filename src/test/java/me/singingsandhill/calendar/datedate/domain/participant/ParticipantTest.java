package me.singingsandhill.calendar.datedate.domain.participant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import me.singingsandhill.calendar.datedate.domain.participant.Participant;

class ParticipantTest {

    @Test
    @DisplayName("Valid participant should be created successfully")
    void validParticipant_createsSuccessfully() {
        Participant participant = new Participant(1L, "Alice", 0);

        assertThat(participant.getName()).isEqualTo("Alice");
        assertThat(participant.getColorHex()).isEqualTo("#E74C3C");
        assertThat(participant.getSelections()).isEmpty();
    }

    @Test
    @DisplayName("Participant with different color index should have different color")
    void differentColorIndex_differentColor() {
        Participant p1 = new Participant(1L, "Alice", 0);
        Participant p2 = new Participant(1L, "Bob", 1);

        assertThat(p1.getColorHex()).isNotEqualTo(p2.getColorHex());
    }

    @Test
    @DisplayName("Color index should wrap around after 8")
    void colorIndex_wrapsAround() {
        Participant p0 = new Participant(1L, "User0", 0);
        Participant p8 = new Participant(1L, "User8", 8);

        assertThat(p0.getColorHex()).isEqualTo(p8.getColorHex());
    }

    @Test
    @DisplayName("Blank name should throw exception")
    void blankName_throwsException() {
        assertThatThrownBy(() -> new Participant(1L, "", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    @DisplayName("Null name should throw exception")
    void nullName_throwsException() {
        assertThatThrownBy(() -> new Participant(1L, null, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    @DisplayName("Name longer than 10 characters should throw exception")
    void longName_throwsException() {
        assertThatThrownBy(() -> new Participant(1L, "VeryLongNameHere", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("10");
    }

    @Test
    @DisplayName("updateSelections should update selections with valid days")
    void updateSelections_validDays() {
        Participant participant = new Participant(1L, "Alice", 0);

        participant.updateSelections(List.of(1, 15, 25), 31);

        assertThat(participant.getSelections()).containsExactly(1, 15, 25);
    }

    @Test
    @DisplayName("updateSelections with day exceeding month days should throw exception")
    void updateSelections_invalidDay_throwsException() {
        Participant participant = new Participant(1L, "Alice", 0);

        assertThatThrownBy(() -> participant.updateSelections(List.of(32), 31))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("updateSelections with day less than 1 should throw exception")
    void updateSelections_dayLessThan1_throwsException() {
        Participant participant = new Participant(1L, "Alice", 0);

        assertThatThrownBy(() -> participant.updateSelections(List.of(0), 31))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("updateSelections should update updatedAt timestamp")
    void updateSelections_updatesTimestamp() {
        Participant participant = new Participant(1L, "Alice", 0);
        var originalUpdatedAt = participant.getUpdatedAt();

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        participant.updateSelections(List.of(1, 2, 3), 31);

        assertThat(participant.getUpdatedAt()).isAfter(originalUpdatedAt);
    }
}
