package me.singingsandhill.calendar.datedate.presentation.controller;

import java.util.List;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import me.singingsandhill.calendar.datedate.domain.guide.GuideSlugs;

/**
 * 모든 뷰 모델에 guides 기사 슬러그 목록을 주입한다.
 *
 * <p>{@code UseCaseNavAdvice} 와 같은 이유 — 푸터가 기사 링크를 하드코딩하지 않게 해서,
 * {@code GuideSlugs} 에 기사를 추가하면 라우팅·사이트맵·푸터가 한 번에 반영된다
 * (고아 페이지 방지).
 */
@ControllerAdvice
public class GuideNavAdvice {

    @ModelAttribute("guideSlugs")
    public List<String> guideSlugs() {
        return GuideSlugs.slugs();
    }
}
