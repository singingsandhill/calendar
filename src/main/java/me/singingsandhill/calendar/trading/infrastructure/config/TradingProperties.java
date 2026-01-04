package me.singingsandhill.calendar.trading.infrastructure.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "trading")
public class TradingProperties {

    private static final Logger log = LoggerFactory.getLogger(TradingProperties.class);

    private Bithumb bithumb = new Bithumb();
    private Bot bot = new Bot();
    private Indicators indicators = new Indicators();
    private Thresholds thresholds = new Thresholds();
    private Risk risk = new Risk();
    private Rebalancing rebalancing = new Rebalancing();

    @PostConstruct
    public void validateConfiguration() {
        // 리밸런싱 설정 검증
        if (rebalancing.getBullRatio() < 0 || rebalancing.getBullRatio() > 1.0) {
            throw new IllegalStateException("Invalid bullRatio: must be between 0 and 1.0");
        }
        if (rebalancing.getBearRatio() < 0 || rebalancing.getBearRatio() > 1.0) {
            throw new IllegalStateException("Invalid bearRatio: must be between 0 and 1.0");
        }
        if (rebalancing.getDefaultRatio() < 0 || rebalancing.getDefaultRatio() > 1.0) {
            throw new IllegalStateException("Invalid defaultRatio: must be between 0 and 1.0");
        }
        if (rebalancing.getDeviationTrigger() <= 0) {
            log.warn("deviationTrigger is {} - rebalancing may trigger too frequently",
                    rebalancing.getDeviationTrigger());
        }
        if (rebalancing.getCooldownMinutes() < 0) {
            throw new IllegalStateException("Invalid cooldownMinutes: must be non-negative");
        }
        if (rebalancing.getMinOrderAmount() < 0) {
            throw new IllegalStateException("Invalid minOrderAmount: must be non-negative");
        }
        if (rebalancing.getSlippageBuffer() < 0 || rebalancing.getSlippageBuffer() > 0.1) {
            log.warn("slippageBuffer {} is outside recommended range (0-0.1)",
                    rebalancing.getSlippageBuffer());
        }
        log.info("TradingProperties validated successfully");
    }

    public static class Bithumb {
        private String baseUrl = "https://api.bithumb.com";
        private String accessKey;
        private String secretKey;

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getAccessKey() { return accessKey; }
        public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
        public String getSecretKey() { return secretKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
    }

    public static class Bot {
        private boolean enabled = false;
        private String market = "KRW-ADA";
        private int maxPositions = 2;
        private double orderRatio = 0.25;
        private double orderRatioMin = 0.15;  // 변동성 높을 때 최소 비율
        private double orderRatioMax = 0.35;  // 변동성 낮을 때 최대 비율

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getMarket() { return market; }
        public void setMarket(String market) { this.market = market; }
        public int getMaxPositions() { return maxPositions; }
        public void setMaxPositions(int maxPositions) { this.maxPositions = maxPositions; }
        public double getOrderRatio() { return orderRatio; }
        public void setOrderRatio(double orderRatio) { this.orderRatio = orderRatio; }
        public double getOrderRatioMin() { return orderRatioMin; }
        public void setOrderRatioMin(double orderRatioMin) { this.orderRatioMin = orderRatioMin; }
        public double getOrderRatioMax() { return orderRatioMax; }
        public void setOrderRatioMax(double orderRatioMax) { this.orderRatioMax = orderRatioMax; }
    }

    public static class Indicators {
        private int maShort = 5;
        private int maMid = 20;
        private int maLong = 60;
        private int rsiPeriod = 14;
        private int stochK = 14;
        private int stochD = 3;
        private int stochSlow = 3;
        private int volumeMa = 20;
        private int atrPeriod = 14;  // ATR 계산 기간

        public int getMaShort() { return maShort; }
        public void setMaShort(int maShort) { this.maShort = maShort; }
        public int getMaMid() { return maMid; }
        public void setMaMid(int maMid) { this.maMid = maMid; }
        public int getMaLong() { return maLong; }
        public void setMaLong(int maLong) { this.maLong = maLong; }
        public int getRsiPeriod() { return rsiPeriod; }
        public void setRsiPeriod(int rsiPeriod) { this.rsiPeriod = rsiPeriod; }
        public int getStochK() { return stochK; }
        public void setStochK(int stochK) { this.stochK = stochK; }
        public int getStochD() { return stochD; }
        public void setStochD(int stochD) { this.stochD = stochD; }
        public int getStochSlow() { return stochSlow; }
        public void setStochSlow(int stochSlow) { this.stochSlow = stochSlow; }
        public int getVolumeMa() { return volumeMa; }
        public void setVolumeMa(int volumeMa) { this.volumeMa = volumeMa; }
        public int getAtrPeriod() { return atrPeriod; }
        public void setAtrPeriod(int atrPeriod) { this.atrPeriod = atrPeriod; }
    }

    public static class Thresholds {
        private int signalBuy = 40;
        private int signalSell = -40;
        private int rsiOversold = 35;
        private int rsiOverbought = 65;
        private int stochOversold = 25;
        private int stochOverbought = 75;
        private int buyRsiMax = 70;
        private int buyStochKMax = 85;
        private int sellRsiMin = 30;
        private int sellStochKMin = 15;

        public int getSignalBuy() { return signalBuy; }
        public void setSignalBuy(int signalBuy) { this.signalBuy = signalBuy; }
        public int getSignalSell() { return signalSell; }
        public void setSignalSell(int signalSell) { this.signalSell = signalSell; }
        public int getRsiOversold() { return rsiOversold; }
        public void setRsiOversold(int rsiOversold) { this.rsiOversold = rsiOversold; }
        public int getRsiOverbought() { return rsiOverbought; }
        public void setRsiOverbought(int rsiOverbought) { this.rsiOverbought = rsiOverbought; }
        public int getStochOversold() { return stochOversold; }
        public void setStochOversold(int stochOversold) { this.stochOversold = stochOversold; }
        public int getStochOverbought() { return stochOverbought; }
        public void setStochOverbought(int stochOverbought) { this.stochOverbought = stochOverbought; }
        public int getBuyRsiMax() { return buyRsiMax; }
        public void setBuyRsiMax(int buyRsiMax) { this.buyRsiMax = buyRsiMax; }
        public int getBuyStochKMax() { return buyStochKMax; }
        public void setBuyStochKMax(int buyStochKMax) { this.buyStochKMax = buyStochKMax; }
        public int getSellRsiMin() { return sellRsiMin; }
        public void setSellRsiMin(int sellRsiMin) { this.sellRsiMin = sellRsiMin; }
        public int getSellStochKMin() { return sellStochKMin; }
        public void setSellStochKMin(int sellStochKMin) { this.sellStochKMin = sellStochKMin; }
    }

    public static class Risk {
        private double stopLoss = -0.08;
        private double takeProfit = 0.15;
        private double trailingStop = 0.03;
        private double trailingActivation = 0.10;
        private double takerFeeRate = 0.0025;        // 0.25% Bithumb taker fee
        private double minProfitThreshold = 0.006;   // 0.6% (왕복 수수료 0.5% + 마진 0.1%)

        public double getStopLoss() { return stopLoss; }
        public void setStopLoss(double stopLoss) { this.stopLoss = stopLoss; }
        public double getTakeProfit() { return takeProfit; }
        public void setTakeProfit(double takeProfit) { this.takeProfit = takeProfit; }
        public double getTrailingStop() { return trailingStop; }
        public void setTrailingStop(double trailingStop) { this.trailingStop = trailingStop; }
        public double getTrailingActivation() { return trailingActivation; }
        public void setTrailingActivation(double trailingActivation) { this.trailingActivation = trailingActivation; }
        public double getTakerFeeRate() { return takerFeeRate; }
        public void setTakerFeeRate(double takerFeeRate) { this.takerFeeRate = takerFeeRate; }
        public double getMinProfitThreshold() { return minProfitThreshold; }
        public void setMinProfitThreshold(double minProfitThreshold) { this.minProfitThreshold = minProfitThreshold; }
    }

    public static class Rebalancing {
        private boolean enabled = true;
        private double defaultRatio = 0.50;
        private double bullRatio = 0.70;
        private double bearRatio = 0.30;
        private double deviationTrigger = 0.10;
        private long cooldownMinutes = 240;              // 4시간 쿨다운
        private double minOrderAmount = 5000.0;          // 최소 주문 금액 (KRW)
        private double slippageBuffer = 0.005;           // 0.5% 슬리피지 버퍼
        private boolean skipWhenDataInsufficient = true; // MA60 데이터 부족 시 스킵

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public double getDefaultRatio() { return defaultRatio; }
        public void setDefaultRatio(double defaultRatio) { this.defaultRatio = defaultRatio; }
        public double getBullRatio() { return bullRatio; }
        public void setBullRatio(double bullRatio) { this.bullRatio = bullRatio; }
        public double getBearRatio() { return bearRatio; }
        public void setBearRatio(double bearRatio) { this.bearRatio = bearRatio; }
        public double getDeviationTrigger() { return deviationTrigger; }
        public void setDeviationTrigger(double deviationTrigger) { this.deviationTrigger = deviationTrigger; }
        public long getCooldownMinutes() { return cooldownMinutes; }
        public void setCooldownMinutes(long cooldownMinutes) { this.cooldownMinutes = cooldownMinutes; }
        public double getMinOrderAmount() { return minOrderAmount; }
        public void setMinOrderAmount(double minOrderAmount) { this.minOrderAmount = minOrderAmount; }
        public double getSlippageBuffer() { return slippageBuffer; }
        public void setSlippageBuffer(double slippageBuffer) { this.slippageBuffer = slippageBuffer; }
        public boolean isSkipWhenDataInsufficient() { return skipWhenDataInsufficient; }
        public void setSkipWhenDataInsufficient(boolean skipWhenDataInsufficient) { this.skipWhenDataInsufficient = skipWhenDataInsufficient; }
    }

    public Bithumb getBithumb() { return bithumb; }
    public void setBithumb(Bithumb bithumb) { this.bithumb = bithumb; }
    public Bot getBot() { return bot; }
    public void setBot(Bot bot) { this.bot = bot; }
    public Indicators getIndicators() { return indicators; }
    public void setIndicators(Indicators indicators) { this.indicators = indicators; }
    public Thresholds getThresholds() { return thresholds; }
    public void setThresholds(Thresholds thresholds) { this.thresholds = thresholds; }
    public Risk getRisk() { return risk; }
    public void setRisk(Risk risk) { this.risk = risk; }
    public Rebalancing getRebalancing() { return rebalancing; }
    public void setRebalancing(Rebalancing rebalancing) { this.rebalancing = rebalancing; }
}
