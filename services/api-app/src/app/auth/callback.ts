import { ChangeDetectionStrategy, Component } from '@angular/core';

/** Landing page of the identity provider's redirect, while the sign-in completes. */
@Component({
  selector: 'app-callback',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<p class="p-8 text-slate-600" role="status">Signing in…</p>`,
})
export class Callback {}
