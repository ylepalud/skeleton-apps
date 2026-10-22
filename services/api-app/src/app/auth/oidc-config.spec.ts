import { oidcConfigFrom } from './oidc-config';

describe('oidcConfigFrom', () => {
  const config = oidcConfigFrom(
    { authority: 'https://id.example/default', clientId: 'web' },
    'https://app.example',
  );

  it('signs in with the authorization code flow and PKCE against the configured provider', () => {
    expect(config.authority).toBe('https://id.example/default');
    expect(config.clientId).toBe('web');
    expect(config.responseType).toBe('code');
    expect(config.redirectUrl).toBe('https://app.example/callback');
  });

  it('renews with refresh tokens, never a hidden iframe', () => {
    expect(config.useRefreshToken).toBe(true);
    expect(config.scope).toContain('offline_access');
    expect(config.ignoreNonceAfterRefresh).toBe(true);
  });

  it('sends the token to the application API only', () => {
    expect(config.secureRoutes).toEqual(['/api/']);
  });
});
