package dev.ylp.config;

import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Every API route requires a bearer token from the OpenID Connect provider; no route is anonymous.
 * Only the Actuator endpoints are open, and they are served on the management port, which the
 * ingress never routes: the kubelet probes them without a token.
 *
 * <p>No CORS configuration: the web app is served from the same origin, so cross-origin calls get
 * no CORS headers and the browser rejects them.
 */
@Configuration
public class SecurityConfiguration {

  /** Opens the Actuator endpoints of the management port. */
  @Bean
  @Order(1)
  public SecurityFilterChain managementSecurity(HttpSecurity http) throws Exception {
    return statelessWithoutCsrf(http)
        .securityMatcher(EndpointRequest.toAnyEndpoint())
        .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
        .build();
  }

  /** Requires a valid JWT on every other request. */
  @Bean
  @Order(2)
  public SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
    return statelessWithoutCsrf(http)
        .authorizeHttpRequests(requests -> requests.anyRequest().authenticated())
        .oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()))
        .build();
  }

  // Bearer tokens, no cookie and no session: CSRF protection has nothing to protect.
  private static HttpSecurity statelessWithoutCsrf(HttpSecurity http) throws Exception {
    return http.sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .csrf(AbstractHttpConfigurer::disable);
  }
}
