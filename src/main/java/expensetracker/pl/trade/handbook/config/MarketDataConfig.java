package expensetracker.pl.trade.handbook.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import expensetracker.pl.trade.handbook.price.MarketDataProperties;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@EnableCaching
@EnableScheduling
@EnableConfigurationProperties(MarketDataProperties.class)
public class MarketDataConfig {

    /**
     * Prototype scope on purpose: {@link RestClient.Builder} is mutable, and each provider
     * sets its own {@code baseUrl} on it.
     */
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public RestClient.Builder marketDataRestClientBuilder() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        return RestClient.builder()
                .requestFactory(factory)
                .defaultHeader("User-Agent", "pl.trade.handbook/0.0.1")
                .defaultHeader("Accept", "application/json");
    }

    @Bean
    public CacheManager cacheManager(MarketDataProperties properties) {
        CaffeineCacheManager manager = new CaffeineCacheManager("quotes", "fxRates");
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(properties.getCacheTtl())
                .maximumSize(properties.getCacheSize()));
        return manager;
    }
}
