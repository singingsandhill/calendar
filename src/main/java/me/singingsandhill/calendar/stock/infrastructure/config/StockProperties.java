package me.singingsandhill.calendar.stock.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "stock")
public class StockProperties {

    private Kis kis = new Kis();
    private Mail mail = new Mail();
    private Bot bot = new Bot();
    private Screening screening = new Screening();
    private Scoring scoring = new Scoring();
    private Entry entry = new Entry();
    private Exit exit = new Exit();
    private Risk risk = new Risk();
    private Trading trading = new Trading();
    private Universe universe = new Universe();

    public static class Kis {
        private String baseUrl = "https://openapi.koreainvestment.com:9443";
        private String appKey;
        private String appSecret;
        private String accountNumber;
        private String accountProductCode = "01";
        private boolean production = true;

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getAppKey() { return appKey; }
        public void setAppKey(String appKey) { this.appKey = appKey; }
        public String getAppSecret() { return appSecret; }
        public void setAppSecret(String appSecret) { this.appSecret = appSecret; }
        public String getAccountNumber() { return accountNumber; }
        public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
        public String getAccountProductCode() { return accountProductCode; }
        public void setAccountProductCode(String accountProductCode) { this.accountProductCode = accountProductCode; }
        public boolean isProduction() { return production; }
        public void setProduction(boolean production) { this.production = production; }
    }

    public static class Mail {
        private boolean enabled = false;
        private String to;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getTo() { return to; }
        public void setTo(String to) { this.to = to; }
    }

    public static class Bot {
        private boolean enabled = false;
        private int maxPositions = 5;
        private BigDecimal maxPositionSize = new BigDecimal("5000000");
        /**
         * 동작 모드:
         *   LIVE     : 실주문 (기본값, 신중)
         *   PAPER    : KIS 시세 사용 + 주문은 인메모리 시뮬레이션
         *   BACKTEST : 모든 시세/주문을 시뮬레이션 (히스토리 fixture)
         */
        private Mode mode = Mode.LIVE;

        public enum Mode { LIVE, PAPER, BACKTEST }

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getMaxPositions() { return maxPositions; }
        public void setMaxPositions(int maxPositions) { this.maxPositions = maxPositions; }
        public BigDecimal getMaxPositionSize() { return maxPositionSize; }
        public void setMaxPositionSize(BigDecimal maxPositionSize) { this.maxPositionSize = maxPositionSize; }
        public Mode getMode() { return mode; }
        public void setMode(Mode mode) { this.mode = mode != null ? mode : Mode.LIVE; }
    }

    public static class Screening {
        private BigDecimal minGapPercent = new BigDecimal("2.0");
        private BigDecimal maxGapPercent = new BigDecimal("7.0");
        private BigDecimal minMarketCap = new BigDecimal("150000000000");
        private BigDecimal minTradeValue = new BigDecimal("500000000");
        private BigDecimal minTradeStrength = new BigDecimal("110");
        private BigDecimal maxSpreadPercent = new BigDecimal("0.3");
        private int maxWatchlistSize = 10;
        private BigDecimal floorGapPercent = new BigDecimal("0.5");
        private BigDecimal floorTradeStrength = new BigDecimal("95");
        /**
         * KIS quote 가 체결강도=0 (장 초반 미집계)을 반환했을 때 동작.
         * true (기본): 데이터 부족으로 스킵 (현행), 안전.
         * false: 0 도 그냥 통과시키고 점수 계산은 floorStrength 로 보정.
         */
        private boolean skipZeroStrength = true;

        public BigDecimal getMinGapPercent() { return minGapPercent; }
        public void setMinGapPercent(BigDecimal minGapPercent) { this.minGapPercent = minGapPercent; }
        public BigDecimal getMaxGapPercent() { return maxGapPercent; }
        public void setMaxGapPercent(BigDecimal maxGapPercent) { this.maxGapPercent = maxGapPercent; }
        public BigDecimal getMinMarketCap() { return minMarketCap; }
        public void setMinMarketCap(BigDecimal minMarketCap) { this.minMarketCap = minMarketCap; }
        public BigDecimal getMinTradeValue() { return minTradeValue; }
        public void setMinTradeValue(BigDecimal minTradeValue) { this.minTradeValue = minTradeValue; }
        public BigDecimal getMinTradeStrength() { return minTradeStrength; }
        public void setMinTradeStrength(BigDecimal minTradeStrength) { this.minTradeStrength = minTradeStrength; }
        public BigDecimal getMaxSpreadPercent() { return maxSpreadPercent; }
        public void setMaxSpreadPercent(BigDecimal maxSpreadPercent) { this.maxSpreadPercent = maxSpreadPercent; }
        public int getMaxWatchlistSize() { return maxWatchlistSize; }
        public void setMaxWatchlistSize(int maxWatchlistSize) { this.maxWatchlistSize = maxWatchlistSize; }
        public BigDecimal getFloorGapPercent() { return floorGapPercent; }
        public void setFloorGapPercent(BigDecimal floorGapPercent) { this.floorGapPercent = floorGapPercent; }
        public BigDecimal getFloorTradeStrength() { return floorTradeStrength; }
        public void setFloorTradeStrength(BigDecimal floorTradeStrength) { this.floorTradeStrength = floorTradeStrength; }
        public boolean isSkipZeroStrength() { return skipZeroStrength; }
        public void setSkipZeroStrength(boolean skipZeroStrength) { this.skipZeroStrength = skipZeroStrength; }
    }

    public static class Scoring {
        private boolean enabled = true;
        private int gapWeight = 30;
        private int strengthWeight = 25;
        private int tradeValueWeight = 20;
        private int spreadWeight = 15;
        private int marketCapWeight = 10;
        private BigDecimal minScoreThreshold = new BigDecimal("40");
        private int minCandidates = 3;
        // 정규화 파라미터 (이전 ScreeningService 하드코딩 상수 → 외부화)
        private BigDecimal gapCenter = new BigDecimal("4.0");
        private BigDecimal gapSigma = new BigDecimal("3.0");
        private BigDecimal strengthMin = new BigDecimal("95");
        private BigDecimal strengthMax = new BigDecimal("130");
        private BigDecimal floorMaxGap = new BigDecimal("15");
        private BigDecimal floorMinMarketCap = new BigDecimal("50000000000");
        private BigDecimal tradeValueMin = new BigDecimal("500000000");      // 5억
        private BigDecimal tradeValueMax = new BigDecimal("50000000000");    // 500억
        private BigDecimal marketCapMin = new BigDecimal("50000000000");     // 500억
        private BigDecimal marketCapMax = new BigDecimal("10000000000000");  // 10조
        private BigDecimal spreadMax = new BigDecimal("0.5");

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getGapWeight() { return gapWeight; }
        public void setGapWeight(int gapWeight) { this.gapWeight = gapWeight; }
        public int getStrengthWeight() { return strengthWeight; }
        public void setStrengthWeight(int strengthWeight) { this.strengthWeight = strengthWeight; }
        public int getTradeValueWeight() { return tradeValueWeight; }
        public void setTradeValueWeight(int tradeValueWeight) { this.tradeValueWeight = tradeValueWeight; }
        public int getSpreadWeight() { return spreadWeight; }
        public void setSpreadWeight(int spreadWeight) { this.spreadWeight = spreadWeight; }
        public int getMarketCapWeight() { return marketCapWeight; }
        public void setMarketCapWeight(int marketCapWeight) { this.marketCapWeight = marketCapWeight; }
        public BigDecimal getMinScoreThreshold() { return minScoreThreshold; }
        public void setMinScoreThreshold(BigDecimal minScoreThreshold) { this.minScoreThreshold = minScoreThreshold; }
        public int getMinCandidates() { return minCandidates; }
        public void setMinCandidates(int minCandidates) { this.minCandidates = minCandidates; }
        public BigDecimal getGapCenter() { return gapCenter; }
        public void setGapCenter(BigDecimal gapCenter) { this.gapCenter = gapCenter; }
        public BigDecimal getGapSigma() { return gapSigma; }
        public void setGapSigma(BigDecimal gapSigma) { this.gapSigma = gapSigma; }
        public BigDecimal getStrengthMin() { return strengthMin; }
        public void setStrengthMin(BigDecimal strengthMin) { this.strengthMin = strengthMin; }
        public BigDecimal getStrengthMax() { return strengthMax; }
        public void setStrengthMax(BigDecimal strengthMax) { this.strengthMax = strengthMax; }
        public BigDecimal getFloorMaxGap() { return floorMaxGap; }
        public void setFloorMaxGap(BigDecimal floorMaxGap) { this.floorMaxGap = floorMaxGap; }
        public BigDecimal getFloorMinMarketCap() { return floorMinMarketCap; }
        public void setFloorMinMarketCap(BigDecimal floorMinMarketCap) { this.floorMinMarketCap = floorMinMarketCap; }
        public BigDecimal getTradeValueMin() { return tradeValueMin; }
        public void setTradeValueMin(BigDecimal tradeValueMin) { this.tradeValueMin = tradeValueMin; }
        public BigDecimal getTradeValueMax() { return tradeValueMax; }
        public void setTradeValueMax(BigDecimal tradeValueMax) { this.tradeValueMax = tradeValueMax; }
        public BigDecimal getMarketCapMin() { return marketCapMin; }
        public void setMarketCapMin(BigDecimal marketCapMin) { this.marketCapMin = marketCapMin; }
        public BigDecimal getMarketCapMax() { return marketCapMax; }
        public void setMarketCapMax(BigDecimal marketCapMax) { this.marketCapMax = marketCapMax; }
        public BigDecimal getSpreadMax() { return spreadMax; }
        public void setSpreadMax(BigDecimal spreadMax) { this.spreadMax = spreadMax; }
    }

    public static class Entry {
        private BigDecimal highThresholdPercent = new BigDecimal("1.5");
        private BigDecimal pullbackMinPercent = new BigDecimal("1.5");
        private BigDecimal pullbackMaxPercent = new BigDecimal("3.0");
        private BigDecimal bounceThresholdPercent = new BigDecimal("0.3");
        private int minPullbackMinutes = 3;
        private int maxPullbackMinutes = 15;
        private BigDecimal entryMinStrength = new BigDecimal("100");
        private BigDecimal entryMinImbalance = new BigDecimal("1.0");
        private boolean softEntryValidation = true;

        public BigDecimal getHighThresholdPercent() { return highThresholdPercent; }
        public void setHighThresholdPercent(BigDecimal highThresholdPercent) { this.highThresholdPercent = highThresholdPercent; }
        public BigDecimal getPullbackMinPercent() { return pullbackMinPercent; }
        public void setPullbackMinPercent(BigDecimal pullbackMinPercent) { this.pullbackMinPercent = pullbackMinPercent; }
        public BigDecimal getPullbackMaxPercent() { return pullbackMaxPercent; }
        public void setPullbackMaxPercent(BigDecimal pullbackMaxPercent) { this.pullbackMaxPercent = pullbackMaxPercent; }
        public BigDecimal getBounceThresholdPercent() { return bounceThresholdPercent; }
        public void setBounceThresholdPercent(BigDecimal bounceThresholdPercent) { this.bounceThresholdPercent = bounceThresholdPercent; }
        public int getMinPullbackMinutes() { return minPullbackMinutes; }
        public void setMinPullbackMinutes(int minPullbackMinutes) { this.minPullbackMinutes = minPullbackMinutes; }
        public int getMaxPullbackMinutes() { return maxPullbackMinutes; }
        public void setMaxPullbackMinutes(int maxPullbackMinutes) { this.maxPullbackMinutes = maxPullbackMinutes; }
        public BigDecimal getEntryMinStrength() { return entryMinStrength; }
        public void setEntryMinStrength(BigDecimal entryMinStrength) { this.entryMinStrength = entryMinStrength; }
        public BigDecimal getEntryMinImbalance() { return entryMinImbalance; }
        public void setEntryMinImbalance(BigDecimal entryMinImbalance) { this.entryMinImbalance = entryMinImbalance; }
        public boolean isSoftEntryValidation() { return softEntryValidation; }
        public void setSoftEntryValidation(boolean softEntryValidation) { this.softEntryValidation = softEntryValidation; }
    }

    public static class Exit {
        private BigDecimal tp1Percent = new BigDecimal("1.5");
        private BigDecimal tp1Ratio = new BigDecimal("0.5");
        private BigDecimal tp2Ratio = new BigDecimal("0.6");
        private BigDecimal tp3Percent = new BigDecimal("1.0");
        private String finalExitTime = "11:20";

        public BigDecimal getTp1Percent() { return tp1Percent; }
        public void setTp1Percent(BigDecimal tp1Percent) { this.tp1Percent = tp1Percent; }
        public BigDecimal getTp1Ratio() { return tp1Ratio; }
        public void setTp1Ratio(BigDecimal tp1Ratio) { this.tp1Ratio = tp1Ratio; }
        public BigDecimal getTp2Ratio() { return tp2Ratio; }
        public void setTp2Ratio(BigDecimal tp2Ratio) { this.tp2Ratio = tp2Ratio; }
        public BigDecimal getTp3Percent() { return tp3Percent; }
        public void setTp3Percent(BigDecimal tp3Percent) { this.tp3Percent = tp3Percent; }
        public String getFinalExitTime() { return finalExitTime; }
        public void setFinalExitTime(String finalExitTime) { this.finalExitTime = finalExitTime; }
    }

    public static class Risk {
        private BigDecimal stopLossPercent = new BigDecimal("1.5");
        private BigDecimal trailingStopPercent = new BigDecimal("0.8");
        private BigDecimal positionSizeRatio = new BigDecimal("0.1");
        private BigDecimal commissionRate = new BigDecimal("0.00015");    // 증권사 수수료 0.015%
        private BigDecimal sellTaxRate = new BigDecimal("0.0023");        // 거래세+농특세 0.23%
        private BigDecimal slippageBuffer = new BigDecimal("0.002");      // 슬리피지 0.2%
        private BigDecimal minProfitThreshold = new BigDecimal("0.005");  // 최소 수익률 0.5%
        private boolean timeDecayEnabled = true;
        private BigDecimal minProfitThresholdLate = new BigDecimal("0.001");  // 장 후반 0.1%

        /** 왕복 수수료율 = 매수 수수료 + 매도 수수료 + 매도 세금 */
        public BigDecimal getRoundTripFeeRate() {
            return commissionRate.multiply(new BigDecimal("2")).add(sellTaxRate);
        }

        public BigDecimal getStopLossPercent() { return stopLossPercent; }
        public void setStopLossPercent(BigDecimal stopLossPercent) { this.stopLossPercent = stopLossPercent; }
        public BigDecimal getTrailingStopPercent() { return trailingStopPercent; }
        public void setTrailingStopPercent(BigDecimal trailingStopPercent) { this.trailingStopPercent = trailingStopPercent; }
        public BigDecimal getPositionSizeRatio() { return positionSizeRatio; }
        public void setPositionSizeRatio(BigDecimal positionSizeRatio) { this.positionSizeRatio = positionSizeRatio; }
        public BigDecimal getCommissionRate() { return commissionRate; }
        public void setCommissionRate(BigDecimal commissionRate) { this.commissionRate = commissionRate; }
        public BigDecimal getSellTaxRate() { return sellTaxRate; }
        public void setSellTaxRate(BigDecimal sellTaxRate) { this.sellTaxRate = sellTaxRate; }
        public BigDecimal getSlippageBuffer() { return slippageBuffer; }
        public void setSlippageBuffer(BigDecimal slippageBuffer) { this.slippageBuffer = slippageBuffer; }
        public BigDecimal getMinProfitThreshold() { return minProfitThreshold; }
        public void setMinProfitThreshold(BigDecimal minProfitThreshold) { this.minProfitThreshold = minProfitThreshold; }
        public boolean isTimeDecayEnabled() { return timeDecayEnabled; }
        public void setTimeDecayEnabled(boolean timeDecayEnabled) { this.timeDecayEnabled = timeDecayEnabled; }
        public BigDecimal getMinProfitThresholdLate() { return minProfitThresholdLate; }
        public void setMinProfitThresholdLate(BigDecimal minProfitThresholdLate) { this.minProfitThresholdLate = minProfitThresholdLate; }
    }

    public static class Universe {
        /**
         * 핀 종목 (항상 유니버스에 포함). 사용자 수동 지정.
         */
        private List<String> pinned = Collections.emptyList();
        /**
         * 정적 안전망 풀 (대형주 + 변동성 종목). 거래량순위 API 실패/0건 시에만 사용.
         */
        private List<String> fallbackCodes = Collections.emptyList();
        /**
         * KIS 거래량순위(FHPST01710000) 결과 상위 N 을 동적 유니버스로 사용. 0 이면 비활성.
         */
        private int rankApiTop = 0;

        public List<String> getPinned() { return pinned; }
        public void setPinned(List<String> pinned) {
            this.pinned = pinned != null ? pinned : Collections.emptyList();
        }
        public List<String> getFallbackCodes() { return fallbackCodes; }
        public void setFallbackCodes(List<String> fallbackCodes) {
            this.fallbackCodes = fallbackCodes != null ? fallbackCodes : Collections.emptyList();
        }
        public int getRankApiTop() { return rankApiTop; }
        public void setRankApiTop(int rankApiTop) { this.rankApiTop = rankApiTop; }
    }

    public static class Trading {
        private String preMarketStart = "08:30";
        private String marketOpen = "09:00";
        /**
         * @deprecated 의미가 모호해 신규 키 {@link #tradingLoopStart} 로 대체. 미설정 시 폴백.
         */
        @Deprecated
        private String screeningEnd = "09:20";
        /**
         * 트레이딩 루프 가드 시작 시각. 보통 스크리닝 cron 시각과 동일.
         * 비워두면 screeningEnd 폴백.
         */
        private String tradingLoopStart;
        private String tradingEnd = "11:30";
        private int pollingIntervalSeconds = 5;
        /**
         * KRX 휴일 (yyyy-MM-dd 문자열). yml 에서 list 로 주입.
         */
        private List<String> holidays = Collections.emptyList();
        private transient Set<LocalDate> holidaySet;

        public String getPreMarketStart() { return preMarketStart; }
        public void setPreMarketStart(String preMarketStart) { this.preMarketStart = preMarketStart; }
        public String getMarketOpen() { return marketOpen; }
        public void setMarketOpen(String marketOpen) { this.marketOpen = marketOpen; }
        public String getScreeningEnd() { return screeningEnd; }
        public void setScreeningEnd(String screeningEnd) { this.screeningEnd = screeningEnd; }
        public String getTradingLoopStart() {
            return tradingLoopStart != null ? tradingLoopStart : screeningEnd;
        }
        public void setTradingLoopStart(String tradingLoopStart) { this.tradingLoopStart = tradingLoopStart; }
        public String getTradingEnd() { return tradingEnd; }
        public void setTradingEnd(String tradingEnd) { this.tradingEnd = tradingEnd; }
        public int getPollingIntervalSeconds() { return pollingIntervalSeconds; }
        public void setPollingIntervalSeconds(int pollingIntervalSeconds) { this.pollingIntervalSeconds = pollingIntervalSeconds; }
        public List<String> getHolidays() { return holidays; }
        public void setHolidays(List<String> holidays) {
            this.holidays = holidays != null ? holidays : Collections.emptyList();
            this.holidaySet = null;
        }

        public boolean isHoliday(LocalDate date) {
            if (holidaySet == null) {
                Set<LocalDate> parsed = new HashSet<>();
                for (String s : holidays) {
                    try {
                        parsed.add(LocalDate.parse(s.trim()));
                    } catch (Exception ignored) {
                        // 잘못된 항목은 무시
                    }
                }
                holidaySet = parsed;
            }
            return holidaySet.contains(date);
        }
    }

    public Universe getUniverse() { return universe; }
    public void setUniverse(Universe universe) { this.universe = universe; }
    public Kis getKis() { return kis; }
    public void setKis(Kis kis) { this.kis = kis; }
    public Mail getMail() { return mail; }
    public void setMail(Mail mail) { this.mail = mail; }
    public Bot getBot() { return bot; }
    public void setBot(Bot bot) { this.bot = bot; }
    public Screening getScreening() { return screening; }
    public void setScreening(Screening screening) { this.screening = screening; }
    public Scoring getScoring() { return scoring; }
    public void setScoring(Scoring scoring) { this.scoring = scoring; }
    public Entry getEntry() { return entry; }
    public void setEntry(Entry entry) { this.entry = entry; }
    public Exit getExit() { return exit; }
    public void setExit(Exit exit) { this.exit = exit; }
    public Risk getRisk() { return risk; }
    public void setRisk(Risk risk) { this.risk = risk; }
    public Trading getTrading() { return trading; }
    public void setTrading(Trading trading) { this.trading = trading; }
}
