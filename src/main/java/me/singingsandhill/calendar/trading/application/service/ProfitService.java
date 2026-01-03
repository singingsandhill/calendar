package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.domain.account.AccountSnapshot;
import me.singingsandhill.calendar.trading.domain.account.AccountSnapshotRepository;
import me.singingsandhill.calendar.trading.domain.account.DailySummary;
import me.singingsandhill.calendar.trading.domain.account.DailySummaryRepository;
import me.singingsandhill.calendar.trading.domain.position.Position;
import me.singingsandhill.calendar.trading.domain.position.PositionRepository;
import me.singingsandhill.calendar.trading.domain.position.PositionStatus;
import me.singingsandhill.calendar.trading.domain.trade.TradeRepository;
import me.singingsandhill.calendar.trading.infrastructure.api.BithumbApiClient;
import me.singingsandhill.calendar.trading.infrastructure.api.dto.BithumbAccountResponse;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ProfitService {

    private static final Logger log = LoggerFactory.getLogger(ProfitService.class);

    private final PositionRepository positionRepository;
    private final TradeRepository tradeRepository;
    private final AccountSnapshotRepository accountSnapshotRepository;
    private final DailySummaryRepository dailySummaryRepository;
    private final BithumbApiClient bithumbApiClient;
    private final TradingProperties tradingProperties;

    public ProfitService(PositionRepository positionRepository,
                         TradeRepository tradeRepository,
                         AccountSnapshotRepository accountSnapshotRepository,
                         DailySummaryRepository dailySummaryRepository,
                         BithumbApiClient bithumbApiClient,
                         TradingProperties tradingProperties) {
        this.positionRepository = positionRepository;
        this.tradeRepository = tradeRepository;
        this.accountSnapshotRepository = accountSnapshotRepository;
        this.dailySummaryRepository = dailySummaryRepository;
        this.bithumbApiClient = bithumbApiClient;
        this.tradingProperties = tradingProperties;
    }

    /**
     * 현재 손익 요약 조회
     */
    public ProfitSummary getProfitSummary() {
        String market = tradingProperties.getBot().getMarket();

        // 현재 잔고 조회
        BithumbAccountResponse krwAccount = bithumbApiClient.getKrwBalance();
        BithumbAccountResponse coinAccount = bithumbApiClient.getCoinBalance();
        Double currentPrice = bithumbApiClient.getCurrentPrice();

        BigDecimal krwBalance = krwAccount != null ? new BigDecimal(krwAccount.balance()) : BigDecimal.ZERO;
        BigDecimal coinBalance = coinAccount != null ? new BigDecimal(coinAccount.balance()) : BigDecimal.ZERO;
        BigDecimal currentPriceBD = currentPrice != null ? BigDecimal.valueOf(currentPrice) : BigDecimal.ZERO;

        // 총 자산 가치
        BigDecimal totalValue = krwBalance.add(coinBalance.multiply(currentPriceBD));

        // 미실현 손익
        Optional<Position> openPosition = positionRepository.findOpenPositionByMarket(market);
        BigDecimal unrealizedPnl = BigDecimal.ZERO;
        BigDecimal unrealizedPnlPct = BigDecimal.ZERO;

        if (openPosition.isPresent() && currentPriceBD.compareTo(BigDecimal.ZERO) > 0) {
            unrealizedPnl = openPosition.get().calculateUnrealizedPnl(currentPriceBD);
            unrealizedPnlPct = openPosition.get().calculateUnrealizedPnlPct(currentPriceBD);
        }

        // 실현 손익 (청산된 포지션)
        List<Position> closedPositions = positionRepository.findByMarketAndStatus(market, PositionStatus.CLOSED);
        BigDecimal realizedPnl = closedPositions.stream()
                .map(Position::getRealizedPnl)
                .filter(p -> p != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 수수료 총액 계산
        BigDecimal totalFeesPaid = closedPositions.stream()
                .map(Position::getTotalFees)
                .filter(f -> f != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 승률 계산
        long totalTrades = closedPositions.size();
        long winningTrades = closedPositions.stream()
                .filter(p -> p.getRealizedPnl() != null && p.getRealizedPnl().compareTo(BigDecimal.ZERO) > 0)
                .count();
        double winRate = totalTrades > 0 ? (double) winningTrades / totalTrades * 100 : 0;

        // 평균 손익
        BigDecimal avgPnlPct = BigDecimal.ZERO;
        if (!closedPositions.isEmpty()) {
            avgPnlPct = closedPositions.stream()
                    .map(Position::getRealizedPnlPct)
                    .filter(p -> p != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(closedPositions.size()), 2, RoundingMode.HALF_UP);
        }

        return new ProfitSummary(
                totalValue, krwBalance, coinBalance, currentPriceBD,
                unrealizedPnl, unrealizedPnlPct,
                realizedPnl, totalFeesPaid, (int) totalTrades, winRate, avgPnlPct
        );
    }

    /**
     * 계좌 스냅샷 저장
     */
    @Transactional
    public void saveAccountSnapshot() {
        String market = tradingProperties.getBot().getMarket();

        BithumbAccountResponse krwAccount = bithumbApiClient.getKrwBalance();
        BithumbAccountResponse coinAccount = bithumbApiClient.getCoinBalance();
        Double currentPrice = bithumbApiClient.getCurrentPrice();

        if (krwAccount == null || coinAccount == null || currentPrice == null) {
            log.warn("Cannot save account snapshot: missing data");
            return;
        }

        BigDecimal krwBalance = new BigDecimal(krwAccount.balance());
        BigDecimal coinBalance = new BigDecimal(coinAccount.balance());
        BigDecimal currentPriceBD = BigDecimal.valueOf(currentPrice);

        // 팩토리 메서드로 생성 (totalValue, adaRatio 등 자동 계산)
        AccountSnapshot snapshot = AccountSnapshot.create(
                krwBalance, coinBalance, null, currentPriceBD
        );

        accountSnapshotRepository.save(snapshot);
        log.debug("Account snapshot saved: total={}", snapshot.getTotalValueKrw());
    }

    /**
     * 일별 요약 생성
     */
    @Transactional
    public void generateDailySummary(LocalDate date) {
        String market = tradingProperties.getBot().getMarket();

        // 해당 일자의 청산된 포지션 조회
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        List<Position> dailyClosedPositions = positionRepository.findByMarketAndStatusAndClosedAtBetween(
                market, PositionStatus.CLOSED, startOfDay, endOfDay);

        int tradeCount = dailyClosedPositions.size();
        int winCount = (int) dailyClosedPositions.stream()
                .filter(p -> p.getRealizedPnl() != null && p.getRealizedPnl().compareTo(BigDecimal.ZERO) > 0)
                .count();

        BigDecimal totalPnl = dailyClosedPositions.stream()
                .map(Position::getRealizedPnl)
                .filter(p -> p != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPnlPct = BigDecimal.ZERO;
        if (tradeCount > 0) {
            totalPnlPct = dailyClosedPositions.stream()
                    .map(Position::getRealizedPnlPct)
                    .filter(p -> p != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(tradeCount), 2, RoundingMode.HALF_UP);
        }

        // 시작/종료 자산 (해당 일자의 첫/마지막 스냅샷)
        BigDecimal startBalance = accountSnapshotRepository.findFirstByMarketAndDateRange(market, startOfDay, endOfDay)
                .map(AccountSnapshot::getTotalValueKrw)
                .orElse(BigDecimal.ZERO);

        BigDecimal endBalance = accountSnapshotRepository.findLastByMarketAndDateRange(market, startOfDay, endOfDay)
                .map(AccountSnapshot::getTotalValueKrw)
                .orElse(startBalance);

        // 팩토리 메서드로 생성 후 데이터 추가
        DailySummary summary = DailySummary.createEmpty(date, startBalance);
        summary.setEndBalance(endBalance);
        for (Position p : dailyClosedPositions) {
            if (p.getRealizedPnl() != null) {
                summary.addClosedPosition(p.getRealizedPnl());
            }
        }

        dailySummaryRepository.save(summary);
        log.info("Daily summary generated for {}: trades={}, pnl={}", date, tradeCount, totalPnl);
    }

    /**
     * 일별 손익 조회
     */
    public List<DailySummary> getDailySummaries(int days) {
        String market = tradingProperties.getBot().getMarket();
        LocalDate fromDate = LocalDate.now().minusDays(days);
        return dailySummaryRepository.findByMarketAndDateAfterOrderByDateDesc(market, fromDate);
    }

    /**
     * 계좌 스냅샷 이력 조회
     */
    public List<AccountSnapshot> getAccountSnapshots(int hours) {
        String market = tradingProperties.getBot().getMarket();
        LocalDateTime fromTime = LocalDateTime.now().minusHours(hours);
        return accountSnapshotRepository.findByMarketAndTimestampAfterOrderByTimestampDesc(market, fromTime);
    }

    public record ProfitSummary(
            BigDecimal totalValue,
            BigDecimal krwBalance,
            BigDecimal coinBalance,
            BigDecimal currentPrice,
            BigDecimal unrealizedPnl,
            BigDecimal unrealizedPnlPct,
            BigDecimal realizedPnl,
            BigDecimal totalFeesPaid,
            int totalTrades,
            double winRate,
            BigDecimal avgPnlPct
    ) {}
}
