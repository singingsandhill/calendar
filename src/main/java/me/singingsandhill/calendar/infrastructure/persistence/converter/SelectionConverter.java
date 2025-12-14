package me.singingsandhill.calendar.infrastructure.persistence.converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SelectionConverter {

    private SelectionConverter() {
    }

    public static String toJson(List<Integer> selections) {
        if (selections == null || selections.isEmpty()) {
            return "[]";
        }
        return "[" + selections.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",")) + "]";
    }

    public static List<Integer> fromJson(String json) {
        if (json == null || json.isBlank() || "[]".equals(json)) {
            return new ArrayList<>();
        }
        String cleaned = json.replace("[", "").replace("]", "").trim();
        if (cleaned.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(cleaned.split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
