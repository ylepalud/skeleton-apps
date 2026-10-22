package dev.ylp.notes.adapter.in.rest;

import dev.ylp.notes.domain.OwnerId;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * The owner of the current request: the subject of its validated JWT. The only way an owner enters
 * a use case, so that no caller can act on behalf of someone else.
 */
@Component
public class CurrentOwner {

  /** Returns the owner of the current request. */
  public OwnerId get() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication instanceof JwtAuthenticationToken token) {
      String subject = token.getToken().getSubject();
      if (subject != null) {
        return new OwnerId(subject);
      }
    }
    // Unreachable through the security configuration, which authenticates every API request.
    throw new IllegalStateException("The request carries no authenticated subject");
  }
}
