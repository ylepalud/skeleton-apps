package dev.ylp.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClockConfigurationTest {

  private final ClockConfiguration configuration = new ClockConfiguration();

  @Test
  @DisplayName("The application clock runs in UTC")
  void applicationClockRunsInUtc() {
    // When / Then
    assertThat(configuration.clock().getZone()).isEqualTo(ZoneOffset.UTC);
  }

  @Test
  @DisplayName("The identifier generator never repeats itself")
  void identifierGeneratorNeverRepeatsItself() {
    // Given
    var generator = configuration.uuidGenerator();

    // When / Then
    assertThat(generator.generate()).isNotEqualTo(generator.generate());
  }
}
