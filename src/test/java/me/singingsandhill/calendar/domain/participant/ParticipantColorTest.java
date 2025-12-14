package me.singingsandhill.calendar.domain.participant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ParticipantColorTest {

    @Test
    @DisplayName("Valid hex color should create ParticipantColor successfully")
    void validHexColor_createsSuccessfully() {
        ParticipantColor color = new ParticipantColor("#E74C3C");

        assertThat(color.hexCode()).isEqualTo("#E74C3C");
    }

    @Test
    @DisplayName("Lowercase hex color should be valid")
    void lowercaseHexColor_isValid() {
        ParticipantColor color = new ParticipantColor("#e74c3c");

        assertThat(color.hexCode()).isEqualTo("#e74c3c");
    }

    @Test
    @DisplayName("Invalid hex color should throw exception")
    void invalidHexColor_throwsException() {
        assertThatThrownBy(() -> new ParticipantColor("E74C3C"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Short hex color should throw exception")
    void shortHexColor_throwsException() {
        assertThatThrownBy(() -> new ParticipantColor("#E74"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Null hex color should throw exception")
    void nullHexColor_throwsException() {
        assertThatThrownBy(() -> new ParticipantColor(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("ofIndex should return preset colors")
    void ofIndex_returnsPresetColors() {
        ParticipantColor color0 = ParticipantColor.ofIndex(0);
        ParticipantColor color1 = ParticipantColor.ofIndex(1);

        assertThat(color0.hexCode()).isEqualTo("#E74C3C");
        assertThat(color1.hexCode()).isEqualTo("#3498DB");
    }

    @Test
    @DisplayName("ofIndex should wrap around for index >= 8")
    void ofIndex_wrapsAround() {
        ParticipantColor color0 = ParticipantColor.ofIndex(0);
        ParticipantColor color8 = ParticipantColor.ofIndex(8);

        assertThat(color0.hexCode()).isEqualTo(color8.hexCode());
    }

    @Test
    @DisplayName("getPresetColorCount should return 8")
    void getPresetColorCount_returns8() {
        assertThat(ParticipantColor.getPresetColorCount()).isEqualTo(8);
    }
}
