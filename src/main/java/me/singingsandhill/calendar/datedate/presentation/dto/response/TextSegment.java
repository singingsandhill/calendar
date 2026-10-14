package me.singingsandhill.calendar.datedate.presentation.dto.response;

/**
 * 일정 설명을 나눈 조각 — {@code url} 이 null 이면 평문, 아니면 http(s) 링크(글자 = URL).
 * 웹은 th:text / th:href 로만 그리고 미니앱은 text 만 이어 붙여 쓴다 (ADR datedate/domain/0011).
 */
public record TextSegment(String text, String url) {
}
