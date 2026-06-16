package com.blood;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class BloodPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(BloodPlatformApplication.class, args);
    }

    // UTC clock injected into services — ensures eligibility is computed consistently
    // regardless of server timezone or client timezone differences.
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
