package dev.ylp.config;

import dev.ylp.shared.id.RandomUUIDGenerator;
import dev.ylp.shared.id.UUIDGenerator;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides time and identity to every module, so that tests can fix both instead of depending on
 * the system clock or on random identifiers.
 */
@Configuration
public class ClockConfiguration {

  /** Returns the UTC system clock. */
  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }

  /** Returns the generator of every entity identifier. */
  @Bean
  public UUIDGenerator uuidGenerator() {
    return new RandomUUIDGenerator();
  }
}
