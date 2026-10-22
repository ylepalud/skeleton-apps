import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import {
  authInterceptor,
  provideAuth,
  StsConfigLoader,
  withAppInitializerAuthCheck,
} from 'angular-auth-oidc-client';
import { client } from './api/generated/client.gen';
import { provideHeyApiClient } from './api/generated/client/client.gen';
import { routes } from './app.routes';
import { runtimeConfigLoader } from './auth/oidc-config';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([authInterceptor()])),
    provideHeyApiClient(client),
    provideAuth(
      { loader: { provide: StsConfigLoader, useFactory: runtimeConfigLoader, deps: [HttpClient] } },
      withAppInitializerAuthCheck(),
    ),
  ],
};
