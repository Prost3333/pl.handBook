package expensetracker.pl.trade.handbook.price;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@ConfigurationProperties(prefix = "market-data")
public class MarketDataProperties {

    private Duration cacheTtl = Duration.ofMinutes(30);
    private int cacheSize = 500;

    private BoerseFrankfurt boerseFrankfurt = new BoerseFrankfurt();
    private AlphaVantage alphaVantage = new AlphaVantage();
    private Fx fx = new Fx();

    @Getter
    @Setter
    public static class BoerseFrankfurt {
        private boolean enabled = true;
        private String baseUrl = "https://api.boerse-frankfurt.de";
        /** exchange suffix -> MIC used by the API. */
        private Map<String, String> micMap = new HashMap<>();
    }

    @Getter
    @Setter
    public static class AlphaVantage {
        private boolean enabled = true;
        private String baseUrl = "https://www.alphavantage.co";
        private String apiKey;
        /** Free tier is 25 calls/day - the provider stops itself before the API does. */
        private int dailyLimit = 25;
        /** exchange suffix -> Alpha Vantage ticker suffix. */
        private Map<String, String> suffixMap = new HashMap<>();
    }

    @Getter
    @Setter
    public static class Fx {
        private boolean enabled = true;
        private String baseUrl = "https://api.frankfurter.dev";
    }
}
