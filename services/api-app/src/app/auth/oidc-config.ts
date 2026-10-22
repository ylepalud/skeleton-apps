import { HttpClient } from '@angular/common/http';
import { OpenIdConfiguration, StsConfigHttpLoader, StsConfigLoader } from 'angular-auth-oidc-client';
import { map } from 'rxjs';

/** What changes between environments, served by the host at /config.json. */
export interface RuntimeConfig {
  readonly authority: string;
  readonly clientId: string;
}

/**
 * Sign-in settings: Authorization Code with PKCE, renewed with refresh tokens rather than a hidden
 * iframe, so the content security policy never has to allow framing the identity provider.
 */
export function oidcConfigFrom(runtime: RuntimeConfig, origin: string): OpenIdConfiguration {
  return {
    authority: runtime.authority,
    clientId: runtime.clientId,
    redirectUrl: `${origin}/callback`,
    postLogoutRedirectUri: origin,
    responseType: 'code',
    scope: 'openid offline_access',
    useRefreshToken: true,
    silentRenew: true,
    // A refreshed id token carries no nonce: there is no authorization request to bind it to.
    ignoreNonceAfterRefresh: true,
    renewTimeBeforeTokenExpiresInSeconds: 30,
    // The bearer token is only ever sent to this application's own API.
    secureRoutes: ['/api/'],
  };
}

/** Loads the sign-in settings from /config.json, so one image runs in every environment. */
export function runtimeConfigLoader(http: HttpClient): StsConfigLoader {
  return new StsConfigHttpLoader(
    http
      .get<RuntimeConfig>('/config.json')
      .pipe(map((runtime) => oidcConfigFrom(runtime, window.location.origin))),
  );
}
