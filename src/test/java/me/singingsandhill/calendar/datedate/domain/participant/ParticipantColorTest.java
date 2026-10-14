package me.singingsandhill.calendar.datedate.domain.participant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import me.singingsandhill.calendar.datedate.domain.participant.ParticipantColor;

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

        assertThat(color0.hexCode()).isEqualTo("#E11D48");
        assertThat(color1.hexCode()).isEqualTo("#F97316");
    }

    @Test
    @DisplayName("ofIndex should wrap around for index >= 10")
    void ofIndex_wrapsAround() {
        ParticipantColor color0 = ParticipantColor.ofIndex(0);
        ParticipantColor color10 = ParticipantColor.ofIndex(10);

        assertThat(color0.hexCode()).isEqualTo(color10.hexCode());
    }

    @Test
    @DisplayName("getPresetColorCount should return 10")
    void getPresetColorCount_returns10() {
        assertThat(ParticipantColor.getPresetColorCount()).isEqualTo(10);
    }

    @Test
    @DisplayName("9·10번째 색은 덧붙인 갈색·청록이다 — 기존 0~7 은 그대로 (ADR datedate/ux/0004)")
    void ofIndex_appendedColors() {
        assertThat(ParticipantColor.ofIndex(8).hexCode()).isEqualTo("#92400E");
        assertThat(ParticipantColor.ofIndex(9).hexCode()).isEqualTo("#0E7490");
    }

    @Test
    @DisplayName("프리셋 색은 모두 서로 다르다")
    void presetColors_areDistinct() {
        List<String> hexes = IntStream.range(0, ParticipantColor.getPresetColorCount())
                .mapToObj(i -> ParticipantColor.ofIndex(i).hexCode())
                .toList();

        assertThat(hexes).doesNotHaveDuplicates();
    }
}
