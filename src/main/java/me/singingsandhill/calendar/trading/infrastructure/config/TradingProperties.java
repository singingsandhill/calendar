package me.singingsandhill.calendar.trading.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "trading")
public class TradingProperties {

    private Bithumb bithumb = new Bithumb();
    private Bot bot = new Bot();
    private Indicators indicators = new Indicators();
    private Thresholds thresholds = new Thresholds();
    private Risk risk = new Risk();
    private Rebalancing rebalancing = new Rebalancing();

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

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getMarket() { return market; }
        public void setMarket(String market) { this.market = market; }
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
    }

    public static class Thresholds {
        private int signalBuy = 50;
        private int signalSell = -50;
        private int rsiOversold = 30;
        private int rsiOverbought = 70;
        private int stochOversold = 20;
        private int stochOverbought = 80;

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
    }

    public static class Risk {
        private double stopLoss = -0.10;
        private double takeProfit = 0.20;
        private double trailingStop = 0.05;

        public double getStopLoss() { return stopLoss; }
        public void setStopLoss(double stopLoss) { this.stopLoss = stopLoss; }
        public double getTakeProfit() { return takeProfit; }
        public void setTakeProfit(double takeProfit) { this.takeProfit = takeProfit; }
        public double getTrailingStop() { return trailingStop; }
        public void setTrailingStop(double trailingStop) { this.trailingStop = trailingStop; }
    }

    public static class Rebalancing {
        private boolean enabled = true;
        private double defaultRatio = 0.50;
        private double bullRatio = 0.70;
        private double bearRatio = 0.30;
        private double deviationTrigger = 0.10;

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
