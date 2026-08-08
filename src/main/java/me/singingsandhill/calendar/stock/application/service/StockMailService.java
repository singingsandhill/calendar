package me.singingsandhill.calendar.stock.application.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import me.singingsandhill.calendar.stock.domain.stock.Stock;
import me.singingsandhill.calendar.stock.infrastructure.config.StockProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Stock 스크리닝 결과 이메일 발송 서비스
 */
@Service
public class StockMailService {

    private static final Logger log = LoggerFactory.getLogger(StockMailService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String LOG_FILE_PATH = "logs/stock-trading.log";

    private final JavaMailSender mailSender;
    private final StockProperties stockProperties;

    public StockMailService(JavaMailSender mailSender, StockProperties stockProperties) {
        this.mailSender = mailSender;
        this.stockProperties = stockProperties;
    }

    /**
     * 스크리닝 결과 이메일 발송
     */
    public void sendScreeningResult(LocalDate tradingDate, List<Stock> stocks,
                                     UniverseBuilder.Snapshot universe) {
        if (!stockProperties.getMail().isEnabled()) {
            log.debug("Stock mail is disabled, skipping email");
            return;
        }

        String to = stockProperties.getMail().getTo();
        if (to == null || to.isBlank()) {
            log.warn("Mail recipient is not configured");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(buildSubject(tradingDate, stocks.size()));
            helper.setText(buildHtmlContent(tradingDate, stocks, universe), true);

            attachLogFile(helper, tradingDate);

            mailSender.send(message);
            log.info("Screening result email sent successfully to {}", to);

        } catch (MessagingException e) {
            log.error("Failed to send screening result email: {}", e.getMessage(), e);
        }
    }

    /**
     * 최종청산 실패 알림 — 청산되지 못한 포지션은 의도치 않은 오버나이트 홀드가 되므로
     * 사람이 수동 청산할 수 있게 즉시 알린다 (2026-07-24 리뷰 §6 / P1-6).
     */
    public void sendTimeExitFailureAlert(LocalDate tradingDate, List<String> stockCodes) {
        if (!stockProperties.getMail().isEnabled()) {
            log.warn("최종청산 실패 알림: 메일 비활성 — 수동 청산 필요 종목 {}", stockCodes);
            return;
        }
        String to = stockProperties.getMail().getTo();
        if (to == null || to.isBlank()) {
            log.warn("최종청산 실패 알림: 수신자 미설정 — 수동 청산 필요 종목 {}", stockCodes);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setTo(to);
            helper.setSubject(String.format("[Stock Bot][긴급] %s 최종청산 실패 %d종목 — 수동 청산 필요",
                tradingDate.format(DATE_FORMATTER), stockCodes.size()));
            helper.setText("<html><body>"
                + "<h2 style='color:#c00;'>최종청산(11:20) 실패</h2>"
                + "<p>아래 종목이 자동 청산되지 않았습니다. HTS/MTS 에서 <strong>수동 청산</strong>하세요.</p>"
                + "<ul><li>" + String.join("</li><li>", stockCodes) + "</li></ul>"
                + "<p style='color:#999;font-size:12px;'>재시도 후에도 실패해 발송된 자동 알림입니다.</p>"
                + "</body></html>", true);
            mailSender.send(message);
            log.info("최종청산 실패 알림 발송: {}", to);
        } catch (MessagingException e) {
            log.error("최종청산 실패 알림 발송 실패: {} (수동 청산 필요 종목 {})", e.getMessage(), stockCodes);
        }
    }

    /**
     * 일일 실적 요약 발송 — PAPER 실측(리뷰 P2-5)의 누적 데이터원.
     */
    public void sendDailyPerformanceReport(DailyPerformanceReportService.DailyReport report) {
        if (!stockProperties.getMail().isEnabled()) {
            log.debug("일일 리포트: 메일 비활성 — 로그로만 기록");
            return;
        }
        String to = stockProperties.getMail().getTo();
        if (to == null || to.isBlank()) {
            log.warn("일일 리포트: 수신자 미설정");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setTo(to);
            helper.setSubject(String.format("[Stock Bot] %s 일일 실적: 청산 %d건, 승률 %s%%, 손익 %s원",
                report.tradingDate().format(DATE_FORMATTER), report.closedCount(),
                report.winRatePercent(), formatWon(report.totalRealizedPnl())));
            helper.setText(buildDailyReportHtml(report), true);
            mailSender.send(message);
            log.info("일일 실적 리포트 발송: {}", to);
        } catch (MessagingException e) {
            log.error("일일 실적 리포트 발송 실패: {}", e.getMessage());
        }
    }

    private String buildDailyReportHtml(DailyPerformanceReportService.DailyReport r) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family: Arial, sans-serif;'>");
        sb.append("<h2>Gap &amp; Pullback 일일 실적 — ").append(r.tradingDate().format(DATE_FORMATTER)).append("</h2>");
        sb.append("<ul>");
        sb.append("<li>청산: ").append(r.closedCount()).append("건 (승 ").append(r.winCount())
            .append(" / 패 ").append(r.lossCount()).append(") — 승률 ").append(r.winRatePercent()).append("%</li>");
        sb.append("<li>실현손익: <strong>").append(formatWon(r.totalRealizedPnl())).append("원</strong></li>");
        sb.append("<li>미청산 잔여: ").append(r.openCount()).append("건</li>");
        sb.append("<li>진입 시도: ").append(r.entryAttemptCount()).append("건</li>");
        sb.append("</ul>");

        appendCountTable(sb, "청산 사유", r.closeReasonCounts());
        appendCountTable(sb, "진입 거절 사유", r.rejectReasonCounts());

        if (!r.pnlByStock().isEmpty()) {
            sb.append("<h3>종목별 실현손익</h3><ul>");
            r.pnlByStock().forEach((code, pnl) ->
                sb.append("<li>").append(code).append(": ").append(formatWon(pnl)).append("원</li>"));
            sb.append("</ul>");
        }

        sb.append("<p style='color:#999;font-size:12px;'>PAPER 실측 누적용 자동 리포트입니다. "
            + "2~4주 누적 후 승률·기대값을 확인하고 LIVE 전환을 판단하세요.</p>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private void appendCountTable(StringBuilder sb, String title, java.util.Map<String, Long> counts) {
        if (counts == null || counts.isEmpty()) {
            return;
        }
        sb.append("<h3>").append(title).append("</h3><ul>");
        counts.forEach((k, v) -> sb.append("<li>").append(k).append(": ").append(v).append("건</li>"));
        sb.append("</ul>");
    }

    private String formatWon(BigDecimal value) {
        if (value == null) return "0";
        return value.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    private String buildSubject(LocalDate tradingDate, int stockCount) {
        String dateStr = tradingDate.format(DATE_FORMATTER);
        if (stockCount == 0) {
            return String.format("[Stock Bot] %s 스크리닝 결과: 조건 충족 종목 없음", dateStr);
        }
        return String.format("[Stock Bot] %s 스크리닝 결과: %d개 종목 감지", dateStr, stockCount);
    }

    private String buildHtmlContent(LocalDate tradingDate, List<Stock> stocks,
                                     UniverseBuilder.Snapshot universe) {
        StringBuilder sb = new StringBuilder();

        sb.append("<html><body style='font-family: Arial, sans-serif;'>");
        sb.append("<h2>Gap & Pullback 스크리닝 결과</h2>");
        sb.append("<p><strong>날짜:</strong> ").append(tradingDate.format(DATE_FORMATTER)).append("</p>");
        // 유니버스 크기는 선정 0~1건의 원인을 가르는 첫 단서다 — 2026-08-03 엔 유니버스가
        // 1종목이었는데 그 사실이 메일 어디에도 없어 스크리닝 임계 문제로 오인될 수 있었다.
        if (universe != null) {
            int size = universe.codes().size();
            // 정상이라면 유니버스는 최소한 정적 안전망만큼은 된다 (rank 미달 시 합집합으로 보강되므로).
            // 그보다 작으면 설정이나 API 응답에 문제가 있는 것이다.
            int expectedMin = stockProperties.getUniverse().getFallbackCodes().size();
            boolean tooSmall = size < expectedMin;
            sb.append("<p").append(tooSmall ? " style='color: #c00; font-weight: bold;'" : "")
                .append("><strong>유니버스:</strong> ")
                .append(size).append("종목 (거래량순위 ").append(universe.rankApi())
                .append(", 정적폴백 ").append(universe.fallback())
                .append(", 핀 ").append(universe.pinned()).append(")");
            if (tooSmall) {
                sb.append(" &larr; 정적 안전망(").append(expectedMin)
                    .append("종목)보다 작습니다. 거래량순위 API 응답과 stock.universe.* 설정을 확인하세요.");
            }
            sb.append("</p>");
        }
        sb.append("<p><strong>감지된 종목:</strong> ").append(stocks.size()).append("개</p>");

        if (stocks.isEmpty()) {
            sb.append("<p style='color: #666;'>오늘 조건을 충족하는 갭 상승 종목이 없습니다.</p>");
        } else {
            sb.append("<h3>종목 리스트</h3>");
            sb.append("<table style='border-collapse: collapse; width: 100%;'>");
            sb.append("<thead><tr style='background-color: #f2f2f2;'>");
            sb.append("<th style='border: 1px solid #ddd; padding: 8px;'>종목코드</th>");
            sb.append("<th style='border: 1px solid #ddd; padding: 8px;'>종목명</th>");
            sb.append("<th style='border: 1px solid #ddd; padding: 8px;'>갭(%)</th>");
            sb.append("<th style='border: 1px solid #ddd; padding: 8px;'>시가총액(억)</th>");
            sb.append("<th style='border: 1px solid #ddd; padding: 8px;'>거래대금(억)</th>");
            sb.append("<th style='border: 1px solid #ddd; padding: 8px;'>체결강도</th>");
            sb.append("<th style='border: 1px solid #ddd; padding: 8px;'>상태</th>");
            sb.append("</tr></thead><tbody>");

            for (Stock stock : stocks) {
                sb.append("<tr>");
                sb.append("<td style='border: 1px solid #ddd; padding: 8px;'>").append(stock.getStockCode()).append("</td>");
                sb.append("<td style='border: 1px solid #ddd; padding: 8px;'>").append(stock.getStockName()).append("</td>");
                sb.append("<td style='border: 1px solid #ddd; padding: 8px; text-align: right;'>")
                    .append(formatPercent(stock.getGapPercent())).append("%</td>");
                sb.append("<td style='border: 1px solid #ddd; padding: 8px; text-align: right;'>")
                    .append(formatBillions(stock.getMarketCap())).append("</td>");
                sb.append("<td style='border: 1px solid #ddd; padding: 8px; text-align: right;'>")
                    .append(formatBillions(stock.getTradeValue())).append("</td>");
                sb.append("<td style='border: 1px solid #ddd; padding: 8px; text-align: right;'>")
                    .append(formatStrength(stock.getTradeStrength())).append("</td>");
                sb.append("<td style='border: 1px solid #ddd; padding: 8px;'>")
                    .append(stock.getState().name()).append("</td>");
                sb.append("</tr>");
            }

            sb.append("</tbody></table>");
        }

        // 실제 적용되는 임계를 인쇄한다. score 모드(기본)와 legacy 모드는 읽는 키가 다르므로
        // 분기하지 않으면 운영자가 적용되지도 않는 값을 보게 된다 — 08-03 에 실효 갭 상한이
        // scoring.floor-max-gap(15%) 인데 메일은 legacy max-gap-percent(10%) 를 인쇄했다.
        StockProperties.Screening screening = stockProperties.getScreening();
        StockProperties.Scoring scoring = stockProperties.getScoring();
        sb.append("<h3>스크리닝 조건</h3>");
        sb.append("<ul>");
        if (scoring.isEnabled()) {
            sb.append("<li>갭 범위(Floor): ").append(screening.getFloorGapPercent())
                .append("% ~ ").append(scoring.getFloorMaxGap()).append("%</li>");
            sb.append("<li>최소 체결강도(Floor): ").append(screening.getFloorTradeStrength()).append("</li>");
            sb.append("<li>최소 시가총액(Floor): ").append(formatBillions(scoring.getFloorMinMarketCap())).append("억</li>");
            sb.append("<li>선정 총점: ").append(scoring.getMinScoreThreshold())
                .append("점 이상 &amp; 신호점수(갭+체결강도): ").append(scoring.getSignalMinScore()).append("점 이상</li>");
        } else {
            sb.append("<li>갭 범위: ").append(screening.getMinGapPercent())
                .append("% ~ ").append(screening.getMaxGapPercent()).append("%</li>");
            sb.append("<li>최소 시가총액: ").append(formatBillions(screening.getMinMarketCap())).append("억</li>");
            sb.append("<li>최소 거래대금: ").append(formatBillions(screening.getMinTradeValue())).append("억</li>");
            sb.append("<li>최소 체결강도: ").append(screening.getMinTradeStrength()).append("</li>");
        }
        sb.append("</ul>");

        sb.append("<p style='color: #999; font-size: 12px;'>첨부 로그는 발송 시점(09:20 스크리닝 직후)까지의 스냅샷입니다 — 이후 트레이딩 구간 로그는 서버의 stock-trading.log 를 확인하세요.</p>");
        sb.append("<p style='color: #999; font-size: 12px;'>이 이메일은 자동 발송되었습니다.</p>");
        sb.append("</body></html>");

        return sb.toString();
    }

    private void attachLogFile(MimeMessageHelper helper, LocalDate tradingDate) {
        File logFile = new File(LOG_FILE_PATH);

        if (logFile.exists() && logFile.length() > 0) {
            try {
                FileSystemResource resource = new FileSystemResource(logFile);
                // logback 롤오버 파일명(stock-trading-YYYY-MM-DD.log)과 겹치면 "하루 전체 로그" 로
                // 오인된다 — 실제로는 발송 시점(09:20)까지의 스냅샷이므로 이름으로 구분.
                String attachmentName = "stock-screening-snapshot-" + tradingDate.format(DATE_FORMATTER) + ".log";
                helper.addAttachment(attachmentName, resource);
                log.debug("Log file attached: {}", LOG_FILE_PATH);
            } catch (MessagingException e) {
                log.warn("Failed to attach log file: {}", e.getMessage());
            }
        } else {
            log.debug("Log file not found or empty: {}", LOG_FILE_PATH);
        }
    }

    private String formatPercent(BigDecimal value) {
        if (value == null) return "-";
        return value.setScale(2, RoundingMode.HALF_UP).toString();
    }

    private String formatBillions(BigDecimal value) {
        if (value == null) return "-";
        return value.divide(new BigDecimal("100000000"), 0, RoundingMode.HALF_UP).toString();
    }

    private String formatStrength(BigDecimal value) {
        if (value == null) return "-";
        return value.setScale(0, RoundingMode.HALF_UP).toString();
    }
}
