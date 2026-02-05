package com.leaky.tokens.analyticsservice;

import com.leaky.tokens.analyticsservice.report.AnalyticsReportProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.kafka.annotation.EnableKafka;

@EnableConfigurationProperties(AnalyticsReportProperties.class)
@SpringBootApplication
public class AnalyticsServiceApplication {
    static void main(String[] args) {
        SpringApplication.run(AnalyticsServiceApplication.class, args);
    }
}
