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
         * 동작 모드 — 분기점은 {@code KoreaInvestmentApiClient} 의 주문 4개 메서드뿐이다.
         * 시세·호가·잔고 조회는 모드와 무관하게 항상 실제 KIS API 를 호출한다.
         *
         *   PAPER    : 실 시세 + 주문만 인메모리 시뮬레이션 (기본값 — LIVE 는 명시적 opt-in)
         *   LIVE     : 실주문 (STOCK_BOT_MODE=LIVE 로만 활성화, ADR stock/modes/0002)
         *   BACKTEST : <b>현재 PAPER 와 동일</b> — 히스토리 fixture 시세 소스는 미구현이며
         *              예약된 값이다. 실측에는 PAPER 를 사용할 것.
         */
        private Mode mode = Mode.PAPER;

        public enum Mode { LIVE, PAPER, BACKTEST }

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getMaxPositions() { return maxPositions; }
        public void setMaxPositions(int maxPositions) { this.maxPositions = maxPositions; }
        public BigDecimal getMaxPositionSize() { return maxPositionSize; }
        public void setMaxPositionSize(BigDecimal maxPositionSize) { this.maxPositionSize = maxPositionSize; }
        public Mode getMode() { return mode; }
        public void setMode(Mode mode) { this.mode = mode != null ? mode : Mode.PAPER; }
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
        /**
         * @deprecated 미사용 — 점수 미달 종목을 강제 선정하던 하한이었다. 엣지 없는 날에도
         * 매일 진입을 시도하게 만들어 제거했다 (ADR stock/algorithm/0008). 키는 하위호환 보존.
         */
        @Deprecated
        private int minCandidates = 3;
        /**
         * 신호 팩터(갭 점수 + 체결강도 점수) 최소 합. 유동성 팩터(거래대금·스프레드·시총 =
         * 최대 45점)만으로 총점 임계(40)를 넘기던 왜곡을 막는다.
         */
        private BigDecimal signalMinScore = new BigDecimal("25");
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
        @Deprecated
        public int getMinCandidates() { return minCandidates; }
        @Deprecated
        public void setMinCandidates(int minCandidates) { this.minCandidates = minCandidates; }
        public BigDecimal getSignalMinScore() { return signalMinScore; }
        public void setSignalMinScore(BigDecimal signalMinScore) { this.signalMinScore = signalMinScore; }
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
        // 기본값은 application.yaml 운영값과 동일하게 유지한다 — yaml 키 누락 시 구식 값으로
        // 조용히 회귀하는 사고 방지 (코인 P1-9 와 동일 원칙).
        private BigDecimal highThresholdPercent = new BigDecimal("1.5");
        private BigDecimal pullbackMinPercent = new BigDecimal("1.5");
        private BigDecimal pullbackMaxPercent = new BigDecimal("5.0");
        private BigDecimal bounceThresholdPercent = new BigDecimal("0.2");
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
        // 기본값은 application.yaml 운영값과 동일하게 유지 (키 누락 시 구식 값 회귀 방지).
        private BigDecimal tp1Percent = new BigDecimal("5.0");
        private BigDecimal tp1Ratio = new BigDecimal("0.5");
        private BigDecimal tp2Ratio = new BigDecimal("0.6");
        private BigDecimal tp3Percent = new BigDecimal("10.0");
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
        /** 레거시 키 — 손절 계산 미사용(아래 앵커+캡 사용). yaml 운영값과 동일하게 유지. */
        private BigDecimal stopLossPercent = new BigDecimal("5.0");
        /** 풀백저가 아래 버퍼(%) — 손절 앵커 = 풀백저가 × (1 - 이 값) */
        private BigDecimal pullbackStopBufferPercent = new BigDecimal("1.0");
        /** 진입가 대비 최대 손실률(%) — 풀백 앵커가 이보다 벌어지면 캡으로 제한 */
        private BigDecimal maxStopLossPercent = new BigDecimal("2.0");
        /**
         * 부분익절 후 러너의 고점 대비 추적폭(%). 손익분기 하한과의 간격이 커지면
         * 트레일이 본전 스탑으로 퇴화한다 — 3.8% 시절 러너 기여 ≈ 0 (ADR algorithm/0009).
         */
        private BigDecimal trailingStopPercent = new BigDecimal("2.0");
        private BigDecimal positionSizeRatio = new BigDecimal("0.1");
        private BigDecimal commissionRate = new BigDecimal("0.00015");    // 증권사 수수료 0.015%
        /**
         * 매도측 세금 (2026-01-01 이후 양도분): 코스피 = 증권거래세 0.05% + 농특세 0.15%,
         * 코스닥·K-OTC = 0.20%(농특세 없음) → 두 시장 모두 0.20%.
         */
        private BigDecimal sellTaxRate = new BigDecimal("0.0020");
        private BigDecimal slippageBuffer = new BigDecimal("0.002");      // 시장가 왕복 슬리피지 0.2%
        private BigDecimal minProfitThreshold = new BigDecimal("0.005");  // 최소 수익률 0.5%
        private boolean timeDecayEnabled = true;
        private BigDecimal minProfitThresholdLate = new BigDecimal("0.001");  // 장 후반 0.1%

        /** 왕복 수수료율 = 매수 수수료 + 매도 수수료 + 매도 세금 */
        public BigDecimal getRoundTripFeeRate() {
            return commissionRate.multiply(new BigDecimal("2")).add(sellTaxRate);
        }

        /**
         * 실효 청산 비용률 = 왕복 수수료·세금 + 시장가 슬리피지.
         *
         * 모든 주문이 시장가라 체결가가 판단 시점 시세에서 밀린다. 익절 게이트가 이 비용을
         * 넘지 못하면 명목상 이익이어도 실제로는 순손실이다 (2026-07-24 리뷰 §4 / P2-1).
         */
        public BigDecimal getEffectiveExitCostRate() {
            return getRoundTripFeeRate().add(slippageBuffer != null ? slippageBuffer : BigDecimal.ZERO);
        }

        public BigDecimal getStopLossPercent() { return stopLossPercent; }
        public void setStopLossPercent(BigDecimal stopLossPercent) { this.stopLossPercent = stopLossPercent; }
        public BigDecimal getPullbackStopBufferPercent() { return pullbackStopBufferPercent; }
        public void setPullbackStopBufferPercent(BigDecimal v) { this.pullbackStopBufferPercent = v; }
        public BigDecimal getMaxStopLossPercent() { return maxStopLossPercent; }
        public void setMaxStopLossPercent(BigDecimal v) { this.maxStopLossPercent = v; }
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
