# Agent Notes (portal-web)

> Scope: this file applies to `portal-web/**` and overrides root notes when they conflict.

## Quick Commands
- Admin dev: `cd api-portal-admin && npm install && npm run dev` (http://localhost:5174)
- Portal dev: `cd api-portal-frontend && npm install && npm run dev` (http://localhost:5173)
- Lint (run inside the app you changed): `npm run lint`
- Build (sanity): `npm run build`
- Portal type-check: `cd api-portal-frontend && npm run type-check`

## Runtime Config
- Both apps use `VITE_API_BASE_URL` as the API prefix used by the Vite dev proxy.
  - `api-portal-admin`: defaults to `/api/v1` (see `api-portal-admin/vite.config.ts`).
  - `api-portal-frontend`: expects `VITE_API_BASE_URL` to be set (see `api-portal-frontend/.env`).
- `api-portal-frontend` proxy target:
  - `VITE_TEMP_API_URL` (default: `http://localhost:8080`) in `api-portal-frontend/vite.config.ts`.
- Multi-tenant portal dev: portal identity may depend on hostname. If needed, add an `/etc/hosts` entry and access the portal via that hostname on `:5173` (see repo root `README.md`).

## Code Style & Conventions
- Lint: ESLint (`eslint.config.js` per app). There is no repo-wide Prettier config here; follow existing formatting.
- Prefer existing structure:
  - `src/pages/` for page-level routes
  - `src/components/` for reusable UI
  - `src/lib/` for API clients/helpers
  - `src/types/` for shared domain types
- Import alias: `@` -> `./src` (see each app's `vite.config.ts`).

## Safety
- Do not edit or commit generated outputs: `dist/`, `node_modules/`.
- Root `.gitignore` ignores `package-lock.json`; do not rely on `npm ci` being reproducible across machines.

## Testing Strategy
This subtree currently has no unified test runner. For any change, treat this as the minimum bar:
- `npm run lint`
- `npm run build`

