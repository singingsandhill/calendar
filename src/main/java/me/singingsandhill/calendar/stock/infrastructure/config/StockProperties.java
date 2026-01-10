package me.singingsandhill.calendar.stock.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "stock")
public class StockProperties {

    private Kis kis = new Kis();
    private Mail mail = new Mail();
    private Bot bot = new Bot();
    private Screening screening = new Screening();
    private Entry entry = new Entry();
    private Exit exit = new Exit();
    private Risk risk = new Risk();
    private Trading trading = new Trading();

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

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getMaxPositions() { return maxPositions; }
        public void setMaxPositions(int maxPositions) { this.maxPositions = maxPositions; }
        public BigDecimal getMaxPositionSize() { return maxPositionSize; }
        public void setMaxPositionSize(BigDecimal maxPositionSize) { this.maxPositionSize = maxPositionSize; }
    }

    public static class Screening {
        private BigDecimal minGapPercent = new BigDecimal("2.0");
        private BigDecimal maxGapPercent = new BigDecimal("7.0");
        private BigDecimal minMarketCap = new BigDecimal("150000000000");
        private BigDecimal minTradeValue = new BigDecimal("500000000");
        private BigDecimal minTradeStrength = new BigDecimal("110");
        private BigDecimal maxSpreadPercent = new BigDecimal("0.3");
        private int maxWatchlistSize = 10;

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
    }

    public static class Entry {
        private BigDecimal highThresholdPercent = new BigDecimal("1.5");
        private BigDecimal pullbackMinPercent = new BigDecimal("1.5");
        private BigDecimal pullbackMaxPercent = new BigDecimal("3.0");
        private BigDecimal bounceThresholdPercent = new BigDecimal("0.3");
        private int minPullbackMinutes = 3;
        private int maxPullbackMinutes = 15;

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

        public BigDecimal getStopLossPercent() { return stopLossPercent; }
        public void setStopLossPercent(BigDecimal stopLossPercent) { this.stopLossPercent = stopLossPercent; }
        public BigDecimal getTrailingStopPercent() { return trailingStopPercent; }
        public void setTrailingStopPercent(BigDecimal trailingStopPercent) { this.trailingStopPercent = trailingStopPercent; }
        public BigDecimal getPositionSizeRatio() { return positionSizeRatio; }
        public void setPositionSizeRatio(BigDecimal positionSizeRatio) { this.positionSizeRatio = positionSizeRatio; }
    }

    public static class Trading {
        private String preMarketStart = "08:30";
        private String marketOpen = "09:00";
        private String screeningEnd = "09:10";
        private String tradingEnd = "11:30";
        private int pollingIntervalSeconds = 5;

        public String getPreMarketStart() { return preMarketStart; }
        public void setPreMarketStart(String preMarketStart) { this.preMarketStart = preMarketStart; }
        public String getMarketOpen() { return marketOpen; }
        public void setMarketOpen(String marketOpen) { this.marketOpen = marketOpen; }
        public String getScreeningEnd() { return screeningEnd; }
        public void setScreeningEnd(String screeningEnd) { this.screeningEnd = screeningEnd; }
        public String getTradingEnd() { return tradingEnd; }
        public void setTradingEnd(String tradingEnd) { this.tradingEnd = tradingEnd; }
        public int getPollingIntervalSeconds() { return pollingIntervalSeconds; }
        public void setPollingIntervalSeconds(int pollingIntervalSeconds) { this.pollingIntervalSeconds = pollingIntervalSeconds; }
    }

    public Kis getKis() { return kis; }
    public void setKis(Kis kis) { this.kis = kis; }
    public Mail getMail() { return mail; }
    public void setMail(Mail mail) { this.mail = mail; }
    public Bot getBot() { return bot; }
    public void setBot(Bot bot) { this.bot = bot; }
    public Screening getScreening() { return screening; }
    public void setScreening(Screening screening) { this.screening = screening; }
    public Entry getEntry() { return entry; }
    public void setEntry(Entry entry) { this.entry = entry; }
    public Exit getExit() { return exit; }
    public void setExit(Exit exit) { this.exit = exit; }
    public Risk getRisk() { return risk; }
    public void setRisk(Risk risk) { this.risk = risk; }
    public Trading getTrading() { return trading; }
    public void setTrading(Trading trading) { this.trading = trading; }
}
