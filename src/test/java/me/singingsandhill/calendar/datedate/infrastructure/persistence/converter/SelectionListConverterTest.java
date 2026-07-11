package me.singingsandhill.calendar.datedate.infrastructure.persistence.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SelectionListConverterTest {

    private final SelectionListConverter converter = new SelectionListConverter();

    @Test
    @DisplayName("null selections serialize to empty array")
    void nullToDb() {
        assertThat(converter.convertToDatabaseColumn(null)).isEqualTo("[]");
    }

    @Test
    @DisplayName("empty list serializes to empty array")
    void emptyToDb() {
        assertThat(converter.convertToDatabaseColumn(new ArrayList<>())).isEqualTo("[]");
    }

    @Test
    @DisplayName("null DB value deserializes to empty list")
    void nullFromDb() {
        assertThat(converter.convertToEntityAttribute(null)).isEmpty();
    }

    @Test
    @DisplayName("blank DB value deserializes to empty list")
    void blankFromDb() {
        assertThat(converter.convertToEntityAttribute("   ")).isEmpty();
    }

    @Test
    @DisplayName("49-day full selection round-trips and stays under VARCHAR(500)")
    void fullExtendedModeRoundtrip() {
        List<Integer> selections = IntStream.rangeClosed(1, 49).boxed().toList();

        String dbValue = converter.convertToDatabaseColumn(selections);
        List<Integer> restored = converter.convertToEntityAttribute(dbValue);

        assertThat(dbValue.length()).isLessThan(500);
        assertThat(restored).containsExactlyElementsOf(selections);
    }

    @Test
    @DisplayName("malformed JSON throws IllegalStateException")
    void malformedFromDb() {
        assertThatThrownBy(() -> converter.convertToEntityAttribute("not-json"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Corrupted selections");
    }
}
