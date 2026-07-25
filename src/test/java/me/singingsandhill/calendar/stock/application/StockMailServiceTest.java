package me.singingsandhill.calendar.stock.application;

import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import me.singingsandhill.calendar.stock.application.service.StockMailService;
import me.singingsandhill.calendar.stock.infrastructure.config.StockProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 스크리닝 결과 메일 회귀 테스트.
 *
 * 첨부 파일명이 logback 롤오버 파일명(stock-trading-YYYY-MM-DD.log)과 동일해서
 * "하루 전체 로그" 로 오인되는 사고가 있었다 (실제로는 발송 시점 09:20 까지의 스냅샷).
 * 기대: 첨부명은 스냅샷임이 드러나는 별도 이름, 본문에 스냅샷 안내 문구 포함.
 */
class StockMailServiceTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);

    @BeforeAll
    static void ensureLogFileExists() throws Exception {
        // 첨부 분기는 logs/stock-trading.log 존재+비어있지 않음 이 전제
        Path logFile = Path.of("logs", "stock-trading.log");
        if (!Files.exists(logFile) || Files.size(logFile) == 0) {
            Files.createDirectories(logFile.getParent());
            Files.writeString(logFile, "test log line\n");
        }
    }

    private StockProperties mailEnabledProps() {
        StockProperties props = new StockProperties();
        props.getMail().setEnabled(true);
        props.getMail().setTo("test@example.com");
        return props;
    }

    @Test
    void attachmentIsNamedAsSnapshotNotAsDailyLogFile() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        StockMailService service = new StockMailService(mailSender, mailEnabledProps());

        service.sendScreeningResult(LocalDate.of(2026, 5, 1), List.of());

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage message = captor.getValue();
        message.saveChanges();

        List<String> fileNames = new ArrayList<>();
        StringBuilder html = new StringBuilder();
        collect(message, fileNames, html);

        assertThat(fileNames).containsExactly("stock-screening-snapshot-2026-05-01.log");
        assertThat(html.toString()).contains("스냅샷");
    }

    private static void collect(Part part, List<String> fileNames, StringBuilder html) throws Exception {
        Object content = part.getContent();
        if (content instanceof Multipart mp) {
            for (int i = 0; i < mp.getCount(); i++) {
                collect(mp.getBodyPart(i), fileNames, html);
            }
        } else if (part.getFileName() != null) {
            fileNames.add(part.getFileName());
        } else if (part.isMimeType("text/html")) {
            html.append(content);
        }
    }
}
