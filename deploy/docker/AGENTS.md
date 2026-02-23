# Agent Notes (deploy/docker)

> Scope: this file applies to `deploy/docker/**` (docker-compose + deploy scripts).

## Quick Commands (Compose)
- Himarket only (with built-in MySQL): `COMPOSE_PROFILES=builtin-mysql docker-compose up -d`
- Full stack (MySQL + Nacos + Redis + Higress + Himarket): `COMPOSE_PROFILES=full-stack,builtin-mysql docker-compose up -d`
- Status: `docker-compose ps`
- Logs: `docker-compose logs -f --tail=200`
- Stop: `docker-compose stop`
- Reset (DANGER: deletes volumes): `docker-compose down -v`

## Config Files
- Compose file: `deploy/docker/docker-compose.yml`
- Compose env (variable interpolation): `deploy/docker/.env`
- Script env (used by `deploy.sh`): `deploy/docker/scripts/data/.env`

## Safety & Conventions
- **Be explicit about your Docker context**
  - Why: `deploy.sh` / compose can create resources and write persistent volumes; running on the wrong context can affect shared environments.
  - Do instead: confirm `docker context show` (and the target host) before `up`/`down`.
  - Verify: after changes, `docker-compose ps` matches what you expect; no unrelated containers are touched.

- **Treat `docker-compose down -v` as data-destructive**
  - Why: it deletes named/anonymous volumes; you may lose MySQL data and any initialized state.
  - Do instead: prefer `docker-compose stop` for temporary shutdowns; back up volumes before reset.
  - Verify: if you intended to keep data, ensure the `deploy/docker/data/` directory remains intact.

- **Do not edit generated data under `deploy/docker/data/`**
  - Why: this is runtime state (MySQL, Higress, etc.), not source-of-truth configuration.
  - Do instead: edit `deploy/docker/docker-compose.yml` and `.env`, or the hook scripts under `deploy/docker/scripts/hooks/`.
  - Verify: config changes are reflected by re-running `docker-compose config` (when available) and restarting services.

## Notes
- Services use Compose profiles:
  - `builtin-mysql`: enables the MySQL container.
  - `full-stack`: enables Nacos/Redis/Higress containers.
- Default host ports in `deploy/docker/docker-compose.yml` include:
  - Admin UI: `5174`
  - Portal UI: `5173`
  - Backend: `8081` (container `8080`)

