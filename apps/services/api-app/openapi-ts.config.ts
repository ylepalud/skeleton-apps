import { defineConfig } from '@hey-api/openapi-ts';

/**
 * Generates the API client from the contract shared with apps/services/api. The client goes
 * through Angular's HttpClient, so interceptors (the bearer token) apply to every call.
 */
export default defineConfig({
  input: '../../contracts/api/openapi.yaml',
  output: 'src/app/api/generated',
  plugins: [
    '@hey-api/typescript',
    '@hey-api/sdk',
    { name: '@hey-api/client-angular', baseUrl: '/api/v1' },
  ],
});
