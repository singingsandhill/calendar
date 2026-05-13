package me.singingsandhill.calendar.common.presentation.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.http.ResponseEntity;

import me.singingsandhill.calendar.common.presentation.dto.response.ErrorResponse;
import me.singingsandhill.calendar.datedate.application.exception.ReservedOwnerIdException;

/**
 * GlobalExceptionHandler 가 BusinessException 의 messageKey 를 현재 로케일로 해석하는지 검증.
 * Reserved owner ID 토스트가 EN 로케일에서 한국어로 노출되던 i18n 회귀 가드.
 */
class GlobalExceptionHandlerI18nTest {

    private MessageSource messageSource;
    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        messageSource = buildMessageSource();
        handler = new GlobalExceptionHandler(messageSource);
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    private static MessageSource buildMessageSource() {
        ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
        ms.setBasename("classpath:messages");
        ms.setDefaultEncoding("UTF-8");
        ms.setFallbackToSystemLocale(false);
        return ms;
    }

    private String expectedMessage(Locale locale, String ownerId) {
        return messageSource.getMessage(
                ReservedOwnerIdException.MESSAGE_KEY,
                new Object[]{ownerId},
                locale);
    }

    @Test
    @DisplayName("ReservedOwnerIdException — KO 로케일에서 한국어 번들 메시지를 반환한다")
    void reservedOwnerId_koreanLocale_returnsKoreanMessage() {
        LocaleContextHolder.setLocale(Locale.KOREAN);

        ResponseEntity<ErrorResponse> response =
                handler.handleBusinessException(new ReservedOwnerIdException("guide"));

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("RESERVED_OWNER_ID");
        assertThat(response.getBody().message())
                .isEqualTo(expectedMessage(Locale.KOREAN, "guide"))
                .contains("guide");
    }

    @Test
    @DisplayName("ReservedOwnerIdException — EN 로케일에서 영어 번들 메시지를 반환한다 (한국어 노출 회귀 가드)")
    void reservedOwnerId_englishLocale_returnsEnglishMessage() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        ResponseEntity<ErrorResponse> response =
                handler.handleBusinessException(new ReservedOwnerIdException("guide"));

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("RESERVED_OWNER_ID");
        assertThat(response.getBody().message())
                .isEqualTo(expectedMessage(Locale.ENGLISH, "guide"))
                .isEqualTo("This ID is taken or reserved: guide");
    }
}
