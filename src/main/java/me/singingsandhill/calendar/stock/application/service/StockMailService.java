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
    public void sendScreeningResult(LocalDate tradingDate, List<Stock> stocks) {
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
            helper.setText(buildHtmlContent(tradingDate, stocks), true);

            attachLogFile(helper, tradingDate);

            mailSender.send(message);
            log.info("Screening result email sent successfully to {}", to);

        } catch (MessagingException e) {
            log.error("Failed to send screening result email: {}", e.getMessage(), e);
        }
    }

    private String buildSubject(LocalDate tradingDate, int stockCount) {
        String dateStr = tradingDate.format(DATE_FORMATTER);
        if (stockCount == 0) {
            return String.format("[Stock Bot] %s 스크리닝 결과: 조건 충족 종목 없음", dateStr);
        }
        return String.format("[Stock Bot] %s 스크리닝 결과: %d개 종목 감지", dateStr, stockCount);
    }

    private String buildHtmlContent(LocalDate tradingDate, List<Stock> stocks) {
        StringBuilder sb = new StringBuilder();

        sb.append("<html><body style='font-family: Arial, sans-serif;'>");
        sb.append("<h2>Gap & Pullback 스크리닝 결과</h2>");
        sb.append("<p><strong>날짜:</strong> ").append(tradingDate.format(DATE_FORMATTER)).append("</p>");
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

        sb.append("<h3>스크리닝 조건</h3>");
        sb.append("<ul>");
        sb.append("<li>갭 범위: ").append(stockProperties.getScreening().getMinGapPercent())
            .append("% ~ ").append(stockProperties.getScreening().getMaxGapPercent()).append("%</li>");
        sb.append("<li>최소 시가총액: ").append(formatBillions(stockProperties.getScreening().getMinMarketCap())).append("억</li>");
        sb.append("<li>최소 거래대금: ").append(formatBillions(stockProperties.getScreening().getMinTradeValue())).append("억</li>");
        sb.append("<li>최소 체결강도: ").append(stockProperties.getScreening().getMinTradeStrength()).append("</li>");
        sb.append("</ul>");

        sb.append("<p style='color: #999; font-size: 12px;'>이 이메일은 자동 발송되었습니다.</p>");
        sb.append("</body></html>");

        return sb.toString();
    }

    private void attachLogFile(MimeMessageHelper helper, LocalDate tradingDate) {
        File logFile = new File(LOG_FILE_PATH);

        if (logFile.exists() && logFile.length() > 0) {
            try {
                FileSystemResource resource = new FileSystemResource(logFile);
                String attachmentName = "stock-trading-" + tradingDate.format(DATE_FORMATTER) + ".log";
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
