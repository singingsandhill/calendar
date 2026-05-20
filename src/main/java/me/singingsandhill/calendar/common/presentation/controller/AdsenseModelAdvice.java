package me.singingsandhill.calendar.common.presentation.controller;

import me.singingsandhill.calendar.common.infrastructure.config.AdsenseProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 모든 Thymeleaf 뷰에 {@code adsense} 모델 속성을 주입한다.
 *
 * <p>head/ad-slot fragment 가 컨트롤러마다 model.addAttribute 호출 없이도
 * {@code ${adsense.client} / ${adsense.hasLeaderboardSlot()}} 등을 참조할 수 있게 한다.
 *
 * <p>{@code @EnableConfigurationProperties} 를 여기에도 둔 이유: {@code @WebMvcTest} 슬라이스
 * 테스트는 {@code @ControllerAdvice} 를 자동으로 로드하지만 {@code @Configuration} 은 건너뛴다.
 * 따라서 {@code AdsenseConfig} 에만 의존하면 슬라이스에서 {@code AdsenseProperties} bean 이
 * 누락돼 advice 생성에 실패한다.
 */
@ControllerAdvice
@EnableConfigurationProperties(AdsenseProperties.class)
public class AdsenseModelAdvice {

    private final AdsenseProperties adsense;

    public AdsenseModelAdvice(AdsenseProperties adsense) {
        this.adsense = adsense;
    }

    @ModelAttribute("adsense")
    public AdsenseProperties adsense() {
        return adsense;
    }
}
