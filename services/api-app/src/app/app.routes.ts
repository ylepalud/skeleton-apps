import { Routes } from '@angular/router';
import { autoLoginPartialRoutesGuard } from 'angular-auth-oidc-client';
import { Callback } from './auth/callback';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'notes' },
  { path: 'callback', component: Callback },
  {
    path: 'notes',
    canActivate: [autoLoginPartialRoutesGuard],
    loadComponent: () => import('./notes/notes-page').then((m) => m.NotesPage),
  },
  { path: '**', redirectTo: 'notes' },
];
