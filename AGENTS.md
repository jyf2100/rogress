# Agent Notes (Himarket)

## Project Overview
Himarket 是一个开箱即用的 AI 开放平台解决方案（管理后台 + 开发者门户 + AI 网关）。本仓库是 **Java/Spring Boot 后端（Maven 多模块）** + **两个 Vite/React 前端（admin 与 portal）** 的组合工程。

## Quick Commands
- Backend build: `mvn clean package`
- Backend run (needs MySQL/MariaDB): `DB_HOST=localhost DB_PORT=3306 DB_NAME=portal_db DB_USERNAME=root DB_PASSWORD=*** java -jar portal-bootstrap/target/portal-bootstrap-*.jar`
- Backend test (all): `mvn test`
- Admin dev: `cd portal-web/api-portal-admin && npm install && npm run dev` (default: http://localhost:5174)
- Portal dev: `cd portal-web/api-portal-frontend && npm install && npm run dev` (default: http://localhost:5173)
- Docker full stack (optional): `cd deploy/docker && COMPOSE_PROFILES=full-stack,builtin-mysql docker-compose up -d`

## Architecture Overview
### Areas
- Backend (Maven modules)
  - `portal-bootstrap/`: Spring Boot 入口 + 打包 Jar + Flyway migrations
  - `portal-server/`: Controller/Service/Security 等业务逻辑
  - `portal-dal/`: JPA Entity/Repository/Converter
- Frontend (Vite apps)
  - `portal-web/api-portal-admin/`: 管理后台（管理员/运营）
  - `portal-web/api-portal-frontend/`: 开发者门户（订阅/测试 API Products）
- Deployment / Docs
  - `deploy/docker/`: docker-compose + 一键部署脚本（可选全栈：MySQL/Nacos/Redis/Higress/Himarket）
  - `docs/apisix/`: APISIX 本地启动配置（用于集成测试）

### Key Entrypoints
- Backend main: `portal-bootstrap/src/main/java/com/alibaba/apiopenplatform/HiMarketApplication.java`
- Backend config: `portal-bootstrap/src/main/resources/application.yml`
- DB migrations (Flyway): `portal-bootstrap/src/main/resources/db/migration/*.sql`
- REST controllers: `portal-server/src/main/java/com/alibaba/apiopenplatform/controller/`
- DAL (entities/repos): `portal-dal/src/main/java/com/alibaba/apiopenplatform/entity/`, `portal-dal/src/main/java/com/alibaba/apiopenplatform/repository/`
- Portal frontend routes + API client:
  - routes: `portal-web/api-portal-frontend/src/router.tsx`
  - API: `portal-web/api-portal-frontend/src/lib/api.ts`

### Data Flow (Local Dev)
```
api-portal-admin / api-portal-frontend (Vite dev server)
  -> calls /api/v1/* (see each app's Vite proxy + .env)
  -> portal-bootstrap (Spring Boot, default :8080)
  -> portal-server (controllers/services)
  -> portal-dal (JPA)
  -> MySQL/MariaDB (portal_db)
```

### Persistence / Ports
- Backend (local): `http://localhost:8080`
  - Swagger UI: `http://localhost:8080/portal/swagger-ui.html`
- Admin UI (local dev): `http://localhost:5174`
- Portal UI (local dev): `http://localhost:5173`
- Docker compose under `deploy/docker/` 可能会把后端映射到 `http://localhost:8081`（以 `deploy/docker/docker-compose.yml` 为准）。

## Runtime Config (Common)
### Backend (Spring Boot)
- DB: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` (见 `portal-bootstrap/src/main/resources/application.yml`)
- 注意：`application.yml` 内含示例/默认值（如 JWT secret、OpenAI key 等）。**不要**把真实密钥写进仓库；用环境变量/启动参数覆盖。

### Frontend (Vite)
- Both apps use `VITE_API_BASE_URL` (default: `/api/v1`) to decide the proxy prefix.
- `api-portal-frontend` additionally supports `VITE_TEMP_API_URL` (default: `http://localhost:8080`) for local proxy target (see `portal-web/api-portal-frontend/vite.config.ts`).

## Code Style & Conventions
### Java (backend)
- Language: Java 17; Framework: Spring Boot 3.2.x; Build: Maven multi-module (`pom.xml` at repo root).
- Keep package layout consistent: `controller/`, `service/`, `dto/params/`, `dto/result/`, `core/security/`, `entity/`, `repository/`.
- DB schema changes: add a new Flyway script under `portal-bootstrap/src/main/resources/db/migration/` (never edit past migrations in-place).

### TypeScript/React (frontend)
- Build tool: Vite; Lint: ESLint (`eslint.config.js` in each app).
- Path alias: `@` -> `./src` (see each app's `vite.config.ts`).
- Prefer the existing app structure (`src/pages`, `src/components`, `src/lib`, `src/types`) and keep API calls centralized in `src/lib/*`.

## Safety & Conventions
- **Do not commit secrets / real credentials**
  - Why: 泄露密钥会造成账号/资源被滥用。
  - Do instead: 用本地 `.env`（gitignored）或环境变量注入；提交前检查 `application.yml`/`.env` 里是否只有示例值。
  - Verify: `git diff` / `git status` 确认无敏感信息；必要时 `rg -n \"api-key|secret|password|token\" .` 自查。

- **Do not run `deploy/docker/scripts/deploy.sh install` against non-dev Docker contexts**
  - Why: 脚本会拉起/改写多个组件并执行 hooks（可能创建默认账号、写入 Nacos/Higress 等）。
  - Do instead: 仅在本机/隔离环境运行；用 `./deploy.sh himarket-only` 进行轻量验证。
  - Verify: 运行前确认当前 Docker context；运行后用 `docker-compose ps` 与 `docker-compose logs -f` 检查。

- **Do not hand-edit DB tables for schema evolution**
  - Why: 手工改表会与 Flyway 迁移脱节，导致其他环境无法复现。
  - Do instead: 新增迁移脚本 `V*_*.sql`，通过应用启动/迁移流程执行。
  - Verify: 新环境启动时 Flyway 能从空库迁移到最新版本；后端正常启动并能提供 API。

- **Do not change generated outputs (`target/`, `dist/`, `node_modules/`)**
  - Why: 生成物不可 review、不可复现，且会污染 diff。
  - Do instead: 修改源文件；通过构建命令生成产物。
  - Verify: `git status` 应保持干净（或仅有源文件变更）。

- **Portal 多租户依赖域名识别：本地调试注意 hosts**
  - Why: 门户域名用于识别 portal（见根目录 `README.md` 说明）。
  - Do instead: 按需配置 `/etc/hosts`，并用自定义域名访问 `:5173`。
  - Verify: 通过浏览器访问时，能正确加载对应 portal 的数据与页面。

## Testing Strategy
### Backend
- Full: `mvn test`
- Module: `mvn test -pl portal-server`
- Single: `mvn test -pl portal-server -Dtest=ApisixClientTest`
- 注意：如果 `mvn -v` 显示的 Java 版本不是 17，请先切到 JDK 17 再跑（macOS 示例：`export JAVA_HOME=$(/usr/libexec/java_home -v 17)`）。
- Integration (APISIX): 默认会跳过；需要先启动 APISIX 并设置环境变量：
  - Start APISIX: `cd docs/apisix && docker-compose up -d`
  - Env: `export APISIX_ADMIN_ENDPOINT=http://localhost:9180` and `export APISIX_ADMIN_KEY=...`
  - Run: `mvn test -pl portal-server -Dtest=ApisixIntegrationTest`

### Frontend
当前仓库未提供统一的自动化测试入口；修改前端代码后至少保证：
- `npm run lint`
- `npm run build`（确保生产构建通过）

### Rule
- 改了代码就要加/改测试（尤其是后端 service/controller），并确保相关测试通过后再合并。

## PR / Review Checklist
- Backend: `mvn test`（或至少 `mvn test -pl portal-server`）
- Frontend: 在改动的 app 目录内跑 `npm run lint` + `npm run build`
- 如涉及 API/行为变更：同步更新 `README.md` 或 `docs/`（以及必要的前端调用/类型定义）

## Scope & Precedence
- 根目录 `AGENTS.md`：默认规则（适用全仓库）。
- 子目录如出现 `AGENTS.md`：对该子目录树生效，并覆盖根目录同主题规则。
- 同目录存在 `AGENTS.override.md` 时优先生效。
- 聊天中的用户显式指令永远优先于文档约定。
