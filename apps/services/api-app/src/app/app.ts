import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterOutlet } from '@angular/router';
import { OidcSecurityService } from 'angular-auth-oidc-client';

/** Application shell: who is signed in, and the current page. */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="border-b border-slate-200 bg-white">
      <div class="mx-auto flex max-w-3xl items-center justify-between px-4 py-3">
        <span class="text-lg font-semibold text-slate-900">Notes</span>
        @if (user(); as name) {
          <div class="flex items-center gap-3 text-sm">
            <span class="text-slate-600">{{ name }}</span>
            <button type="button" class="btn-secondary" (click)="signOut()">Sign out</button>
          </div>
        }
      </div>
    </header>
    <main class="mx-auto max-w-3xl px-4 py-6">
      <router-outlet />
    </main>
  `,
})
export class App {
  private readonly oidc = inject(OidcSecurityService);
  private readonly userData = toSignal(this.oidc.userData$);

  /** Subject of the signed-in user, or nothing while signed out. */
  protected readonly user = computed(
    () => (this.userData()?.userData as { sub?: string } | null | undefined)?.sub,
  );

  protected signOut(): void {
    this.oidc.logoffAndRevokeTokens().subscribe();
  }
}
