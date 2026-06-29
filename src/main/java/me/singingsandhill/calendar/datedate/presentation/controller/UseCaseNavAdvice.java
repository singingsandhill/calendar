package me.singingsandhill.calendar.datedate.presentation.controller;

import java.util.List;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import me.singingsandhill.calendar.datedate.domain.usecase.UseCaseSlugs;

/**
 * 모든 Thymeleaf 뷰에 {@code useCaseSlugs} 모델 속성을 주입한다.
 *
 * <p>{@code AdsenseModelAdvice} 와 동일 패턴. 푸터(footer.html) 가 use-case 링크를
 * 하드코딩하지 않고 {@link UseCaseSlugs#ALL} 을 {@code th:each} 로 순회하게 하여,
 * 슬러그를 추가하면 라우팅·사이트맵·푸터가 한 번에 반영되도록 한다 (고아 페이지 방지).
 */
@ControllerAdvice
public class UseCaseNavAdvice {

    @ModelAttribute("useCaseSlugs")
    public List<String> useCaseSlugs() {
        return UseCaseSlugs.ALL;
    }
}
