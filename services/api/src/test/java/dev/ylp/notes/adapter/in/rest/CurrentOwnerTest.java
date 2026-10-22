package dev.ylp.notes.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.ylp.notes.domain.OwnerId;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class CurrentOwnerTest {

  private final CurrentOwner currentOwner = new CurrentOwner();

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private static Jwt.Builder aJwt() {
    return Jwt.withTokenValue("token")
        .header("alg", "RS256")
        .issuedAt(Instant.EPOCH)
        .expiresAt(Instant.EPOCH.plusSeconds(60));
  }

  @Test
  @DisplayName("The owner is the subject of the request's token")
  void ownerIsTheSubjectOfTheRequestsToken() {
    // Given
    SecurityContextHolder.getContext()
        .setAuthentication(new JwtAuthenticationToken(aJwt().subject("ada").build()));

    // When / Then
    assertThat(currentOwner.get()).isEqualTo(new OwnerId("ada"));
  }

  @Test
  @DisplayName("A token without subject has no owner")
  void tokenWithoutSubjectHasNoOwner() {
    // Given
    SecurityContextHolder.getContext()
        .setAuthentication(new JwtAuthenticationToken(aJwt().claim("scope", "x").build()));

    // When / Then
    assertThatThrownBy(currentOwner::get).isInstanceOf(IllegalStateException.class);
  }

  @Test
  @DisplayName("A request not authenticated by a token has no owner")
  void requestNotAuthenticatedByTokenHasNoOwner() {
    // Given
    SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("u", "p"));

    // When / Then
    assertThatThrownBy(currentOwner::get).isInstanceOf(IllegalStateException.class);
  }
}
