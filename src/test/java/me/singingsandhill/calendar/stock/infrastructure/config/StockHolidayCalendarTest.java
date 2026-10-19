package me.singingsandhill.calendar.stock.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 운영 application.yaml 의 KRX 휴장일 목록 가드.
 *
 * stock.trading.holidays 는 도입(5a5be58) 이후 비어 있어, 평일 휴장일에도 08:30~11:40 잡이 전부 돌았다.
 * isHoliday() 는 파싱 실패 항목을 조용히 무시하므로 오탈자는 "그날만 차단 안 됨" 으로만 드러난다 —
 * 형식과 요일(주말 항목 = 오탈자 신호)을 여기서 기계 검증한다.
 */
class StockHolidayCalendarTest {

    private static StockProperties.Trading operationalTrading() throws Exception {
        List<PropertySource<?>> documents =
            new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yaml"));
        // 첫 문서(프로필 없음)가 운영 기본값
        return new Binder(ConfigurationPropertySources.from(documents.get(0)))
            .bind("stock.trading", StockProperties.Trading.class)
            .get();
    }

    @Test
    void everyHolidayEntryIsAValidWeekdayDate() throws Exception {
        List<String> holidays = operationalTrading().getHolidays();

        assertThat(holidays).isNotEmpty().doesNotHaveDuplicates();
        for (String h : holidays) {
            LocalDate date = LocalDate.parse(h);  // 형식 오류면 여기서 실패
            assertThat(date.getDayOfWeek()).as(h).isNotIn(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
        }
    }

    @Test
    void knownClosuresAreHolidays_regularDayIsNot() throws Exception {
        StockProperties.Trading trading = operationalTrading();

        assertThat(trading.isHoliday(LocalDate.of(2026, 12, 25))).isTrue();   // 성탄절
        assertThat(trading.isHoliday(LocalDate.of(2026, 12, 31))).isTrue();   // 연말 휴장
        assertThat(trading.isHoliday(LocalDate.of(2027, 5, 3))).isTrue();     // 노동절 대체공휴일
        assertThat(trading.isHoliday(LocalDate.of(2027, 7, 19))).isTrue();    // 제헌절 대체공휴일
        assertThat(trading.isHoliday(LocalDate.of(2026, 12, 24))).isFalse();
    }
}
