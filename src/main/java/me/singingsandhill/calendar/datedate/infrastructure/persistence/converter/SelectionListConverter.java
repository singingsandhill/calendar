package me.singingsandhill.calendar.datedate.infrastructure.persistence.converter;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class SelectionListConverter implements AttributeConverter<List<Integer>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<Integer>> LIST_TYPE = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(List<Integer> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize selections: " + attribute, e);
        }
    }

    @Override
    public List<Integer> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return MAPPER.readValue(dbData, LIST_TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Corrupted selections value in database: " + dbData, e);
        }
    }
}
