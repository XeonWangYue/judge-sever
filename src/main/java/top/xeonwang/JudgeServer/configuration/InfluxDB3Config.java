package top.xeonwang.JudgeServer.configuration;

import com.influxdb.v3.client.InfluxDBClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InfluxDB3Config {

    @Value("${spring.influxdb3.url}")
    private String url;

    @Value("${spring.influxdb3.token}")
    private char[] token;

    @Bean(destroyMethod = "close")
    public InfluxDBClient rankTsClient() {
        return InfluxDBClient.getInstance(url, token, "rank");
    }
}