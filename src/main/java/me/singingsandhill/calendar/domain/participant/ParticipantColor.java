package me.singingsandhill.calendar.domain.participant;

import java.util.List;

public record ParticipantColor(String hexCode) {

    private static final List<String> PRESET_COLORS = List.of(
            "#E74C3C", // Red
            "#3498DB", // Blue
            "#2ECC71", // Green
            "#F39C12", // Orange
            "#9B59B6", // Purple
            "#1ABC9C", // Teal
            "#E67E22", // Dark Orange
            "#34495E"  // Dark
    );

    public ParticipantColor {
        if (hexCode == null || !hexCode.matches("^#[0-9A-Fa-f]{6}$")) {
            throw new IllegalArgumentException("Invalid hex color code: " + hexCode);
        }
    }

    public static ParticipantColor ofIndex(int index) {
        return new ParticipantColor(PRESET_COLORS.get(index % PRESET_COLORS.size()));
    }

    public static int getPresetColorCount() {
        return PRESET_COLORS.size();
    }
}
