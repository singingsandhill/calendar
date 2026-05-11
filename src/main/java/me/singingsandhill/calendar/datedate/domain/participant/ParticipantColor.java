package me.singingsandhill.calendar.datedate.domain.participant;

import java.util.List;

public record ParticipantColor(String hexCode) {

    private static final List<String> PRESET_COLORS = List.of(
            "#E11D48", // Red    — rose-600
            "#F97316", // Orange — orange-500
            "#CA8A04", // Yellow — amber-700 (AA against white as visual indicator)
            "#16A34A", // Green  — green-600
            "#0D9488", // Teal   — teal-600
            "#2563EB", // Blue   — blue-600
            "#4F46E5", // Indigo — indigo-600
            "#7C3AED"  // Violet — violet-600
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
