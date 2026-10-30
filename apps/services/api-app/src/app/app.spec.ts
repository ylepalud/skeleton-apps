import { provideRouter } from '@angular/router';
import { render, screen } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';
import { OidcSecurityService } from 'angular-auth-oidc-client';
import { of } from 'rxjs';
import { App } from './app';

describe('App', () => {
  it('shows who is signed in and signs them out', async () => {
    const logoffAndRevokeTokens = vi.fn(() => of(undefined));
    await render(App, {
      providers: [
        provideRouter([]),
        { provide: OidcSecurityService, useValue: { userData$: of({ userData: { sub: 'ada' } }), logoffAndRevokeTokens } },
      ],
    });

    expect(screen.getByText('ada')).toBeInTheDocument();
    await userEvent.setup().click(screen.getByRole('button', { name: 'Sign out' }));
    expect(logoffAndRevokeTokens).toHaveBeenCalled();
  });

  it('shows no user while signed out', async () => {
    await render(App, {
      providers: [
        provideRouter([]),
        { provide: OidcSecurityService, useValue: { userData$: of({ userData: null }), logoffAndRevokeTokens: vi.fn() } },
      ],
    });

    expect(screen.queryByRole('button', { name: 'Sign out' })).not.toBeInTheDocument();
  });
});
