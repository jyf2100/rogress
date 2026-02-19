# HiMarket 多网关统一架构 - APISIX 集成可行性研究

> 计划 ID: v1-multi-gateway-01
> 创建日期: 2025-02-18
> 状态: 分析阶段

---

## 1. 分析 (Analysis)

### 1.1 现状

**已有架构**：
```
GatewayOperator (抽象基类)
├── APIGOperator     → 云原生 API 网关 (APIG_API)
├── AIGWOperator     → AI 网关 (APIG_AI)，继承 APIGOperator
├── HigressOperator  → Higress 网关 (HIGRESS)
├── AdpAIGatewayOperator → ADP AI 网关
└── ApsaraGatewayOperator → Apsara AI 网关
```

**核心文件**：
| 文件 | 路径 | 说明 |
|------|------|------|
| GatewayOperator | `portal-server/.../gateway/GatewayOperator.java` | 抽象基类 |
| GatewayType | `portal-dal/.../enums/GatewayType.java` | 网关类型枚举 |
| GatewayConfig | `portal-dal/.../gateway/GatewayConfig.java` | 配置容器 |
| GatewayServiceImpl | `portal-server/.../impl/GatewayServiceImpl.java` | Spring 自动发现 |

**MCP 支持现状**：
| 网关 | MCP 支持 | 传输类型 | 实现方式 |
|------|---------|---------|---------|
| APIG_AI | ✅ | SSE, HTTP-to-MCP | SDK 调用 |
| HIGRESS | ✅ | SSE, HTTP | REST API `/v1/mcpServer` |
| ADP_AI_GATEWAY | ✅ | SSE | REST API |
| APSARA_GATEWAY | ✅ | SSE | SDK 调用 |

### 1.2 约束

1. **不破坏现有接口**：`GatewayOperator` 抽象方法签名不变
2. **兼容现有配置**：`Gateway` 实体、`GatewayConfig` 结构可扩展
3. **Spring 自动发现机制**：新 Operator 必须遵循 `@Service` + `getGatewayType()` 模式
4. **APISIX Admin API**：需要 HTTP 调用，不能依赖 SDK
5. **mcp-bridge 插件限制**：仅支持 stdio 类型的 MCP Server

### 1.3 成功标准

| 标准 | 验证方式 |
|------|---------|
| ApisixOperator 可被 Spring 自动发现 | 单元测试：`applicationContext.getBeansOfType(GatewayOperator.class)` 包含 APISIX |
| 支持基础路由配置 | 集成测试：创建/删除 Route |
| 支持 MCP Server 注册 | 集成测试：通过 mcp-bridge 插件配置 MCP Server |
| 与 HigressOperator 功能对等 | 功能对比测试：MCP Server 列表/配置/授权 |

### 1.4 风险

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|---------|
| mcp-bridge 插件不稳定 | 中 | 高 | 版本锁定 + 回退方案 |
| APISIX 进程管理能力有限 | 高 | 中 | 外部进程管理器 |
| Admin API 认证复杂度 | 低 | 低 | 支持 API Key 认证 |
| MCP Server 健康检查缺失 | 高 | 中 | 自建健康检查机制 |

---

## 2. 设计 (Design)

### 方案 A：完整集成（推荐）

**核心思路**：实现完整的 `ApisixOperator`，支持路由、MCP、Consumer 管理

```
ApisixOperator
├── ApisixClient (HTTP 客户端)
├── ApisixConfig (配置类)
├── ApisixConfigConverter (JPA 转换器)
└── 支持的能力:
    ├── Route CRUD
    ├── mcp-bridge 插件配置
    ├── Consumer 管理
    └── 健康检查 (自建)
```

**优点**：
- 功能完整，与 Higress 对等
- 可复用现有架构模式
- 易于维护和扩展

**缺点**：
- 开发工作量较大
- mcp-bridge 进程管理需要额外处理

### 方案 B：最小集成

**核心思路**：仅实现 MCP 相关功能，复用 APISIX 现有路由配置

```
ApisixOperator (MCP Only)
├── ApisixClient (仅 MCP 相关 API)
├── mcp-bridge 插件配置
└── 其他操作抛出 UnsupportedOperationException
```

**优点**：
- 开发快速
- 风险可控

**缺点**：
- 功能不完整
- 用户体验不一致

### 方案 C：混合代理

**核心思路**：通过 Higress 代理 APISIX 的 MCP 服务

```
HiMarket → Higress → APISIX (mcp-bridge) → MCP Server
```

**优点**：
- 无需修改 HiMarket
- 利用 Higress 成熟能力

**缺点**：
- 架构复杂
- 多一层代理延迟
- 不能直接管理 APISIX

### 推荐方案

**推荐方案 A（完整集成）**

理由：
1. 架构一致性：与现有 Operator 模式完全对齐
2. 功能对等：支持 Higress 所有能力
3. 长期维护：代码结构清晰，易于维护
4. 扩展性：后续可添加更多 APISIX 特有功能

---

## 3. 落地计划 (Plan)

### Phase 1: 基础架构（预计 2 周）

#### Task 1.1: 扩展枚举和配置类

**Files**:
- Create: `portal-dal/.../gateway/ApisixConfig.java`
- Modify: `portal-dal/.../enums/GatewayType.java`
- Modify: `portal-dal/.../gateway/GatewayConfig.java`
- Test: `portal-dal/src/test/.../ApisixConfigTest.java`

**Step 1**: 写失败测试
```java
// ApisixConfigTest.java
@Test
void testGatewayTypeContainsApisix() {
    GatewayType type = GatewayType.valueOf("APISIX");
    assertNotNull(type);
    assertEquals("APISIX", type.getType());
}
```

**Step 2**: 运行测试 → 失败 (APISIX 不存在)

**Step 3**: 最小实现
```java
// GatewayType.java 添加
APISIX("APISIX");

// ApisixConfig.java 新建
@Data
public class ApisixConfig {
    private String adminApiEndpoint;  // http://apisix-admin:9180
    private String adminApiKey;
    private Integer timeout = 30000;
}
```

**Step 4**: 运行测试 → 通过

---

#### Task 1.2: 实现 ApisixClient

**Files**:
- Create: `portal-server/.../gateway/client/ApisixClient.java`
- Test: `portal-server/src/test/.../ApisixClientTest.java`

**Step 1**: 写失败测试
```java
@Test
void testCreateRoute() {
    ApisixConfig config = new ApisixConfig();
    config.setAdminApiEndpoint("http://localhost:9180");
    config.setAdminApiKey("test-key");

    ApisixClient client = new ApisixClient(config);

    RouteRequest route = RouteRequest.builder()
        .id("test-route")
        .uri("/test/*")
        .build();

    assertDoesNotThrow(() -> client.createRoute(route));
}
```

**Step 2**: 运行测试 → 失败 (ApisixClient 不存在)

**Step 3**: 最小实现 (参考 HigressClient 模式)

**Step 4**: 运行测试 → 通过

---

#### Task 1.3: 实现 ApisixOperator 基础

**Files**:
- Create: `portal-server/.../gateway/ApisixOperator.java`
- Test: `portal-server/src/test/.../ApisixOperatorTest.java`

**Step 1**: 写失败测试
```java
@Test
void testGetGatewayType() {
    ApisixOperator operator = new ApisixOperator();
    assertEquals(GatewayType.APISIX, operator.getGatewayType());
}

@Test
void testSpringAutoDiscovery() {
    // 验证 Spring 自动发现
    Map<String, GatewayOperator> operators = applicationContext.getBeansOfType(GatewayOperator.class);
    assertTrue(operators.values().stream()
        .anyMatch(o -> o.getGatewayType() == GatewayType.APISIX));
}
```

**Step 2**: 运行测试 → 失败

**Step 3**: 最小实现
```java
@Service
public class ApisixOperator extends GatewayOperator<ApisixClient> {

    @Override
    public GatewayType getGatewayType() {
        return GatewayType.APISIX;
    }

    // 其他方法先抛出 UnsupportedOperationException
}
```

**Step 4**: 运行测试 → 通过

---

### Phase 2: MCP 集成（预计 1 周）

#### Task 2.1: 实现 mcp-bridge 配置

**Files**:
- Modify: `portal-server/.../gateway/ApisixOperator.java`
- Create: `portal-server/.../gateway/model/ApisixMcpBridgeConfig.java`
- Test: `portal-server/src/test/.../ApisixMcpTest.java`

**mcp-bridge 插件配置结构**:
```json
{
  "uri": "/mcp/{serverName}/*",
  "plugins": {
    "mcp-bridge": {
      "command": "/usr/local/bin/mcp-server-filesystem",
      "args": ["--root", "/data/files"]
    }
  }
}
```

**Step 1**: 写失败测试
```java
@Test
void testRegisterMcpServer() {
    Gateway gateway = createTestGateway(GatewayType.APISIX);
    McpServerConfig config = McpServerConfig.builder()
        .serverId("filesystem")
        .transportType(McpTransportType.STDIO)
        .command("/usr/local/bin/mcp-server-filesystem")
        .args(List.of("--root", "/data/files"))
        .build();

    PageResult<? extends GatewayMCPServerResult> result =
        operator.fetchMcpServers(gateway, 1, 10);

    assertNotNull(result);
}
```

**Step 2**: 运行测试 → 失败 (fetchMcpServers 未实现)

**Step 3**: 最小实现
```java
@Override
public PageResult<? extends GatewayMCPServerResult> fetchMcpServers(
        Gateway gateway, int page, int size) {
    ApisixClient client = getClient(gateway);

    // 获取所有 Route，筛选带 mcp-bridge 插件的
    List<ApisixRoute> routes = client.listRoutes();

    List<ApisixMCPServerResult> mcpServers = routes.stream()
        .filter(r -> r.getPlugins().containsKey("mcp-bridge"))
        .map(this::convertToMcpServerResult)
        .collect(Collectors.toList());

    return PageResult.of(mcpServers, page, size, mcpServers.size());
}
```

**Step 4**: 运行测试 → 通过

---

### Phase 3: 完善与测试（预计 1 周）

#### Task 3.1: Consumer 管理

#### Task 3.2: 健康检查机制

#### Task 3.3: 集成测试

---

## 4. 工作量评估

| Phase | 任务 | 预估工时 | 优先级 |
|-------|------|---------|--------|
| Phase 1.1 | 枚举和配置类 | 2h | P0 |
| Phase 1.2 | ApisixClient | 6h | P0 |
| Phase 1.3 | ApisixOperator 基础 | 8h | P0 |
| Phase 2.1 | mcp-bridge 配置 | 6h | P0 |
| Phase 2.2 | MCP Server CRUD | 8h | P0 |
| Phase 2.3 | Consumer 管理 | 4h | P1 |
| Phase 3.1 | 健康检查 | 6h | P1 |
| Phase 3.2 | 集成测试 | 8h | P1 |
| **总计** | | **48h** | |

---

## 5. 决策记录

| 决策点 | 选择 | 说明 |
|--------|------|------|
| APISIX 部署 | B. 需要新建 | 后续需要搭建测试环境 |
| mcp-bridge 插件 | B. 需要安装 | 随 APISIX 部署时安装 |
| 实施优先级 | 基础路由 → MCP | 先完成 Operator 基础架构 |
| 时间要求 | A. 立即开始 | 2025-02-18 开始执行 |

---

## 6. 执行进度

### Phase 1 Task 1.1: 扩展枚举和配置类 ✅

**状态**: ✅ 已完成并验证通过

**提交**: `806b634` - feat(gateway): add APISIX gateway type and config support

**推送**: https://github.com/jyf2100/rogress (main 分支)

**测试验证** (JDK 17):
```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
mvn test -pl portal-server -Dtest=ApisixConfigTest
# Tests run: 7, Failures: 0, Errors: 0, Skipped: 0 - BUILD SUCCESS
```

**变更文件**:
- Create: `portal-dal/.../support/gateway/ApisixConfig.java`
- Create: `portal-dal/.../converter/ApisixConfigConverter.java`
- Modify: `portal-dal/.../enums/GatewayType.java` (添加 APISIX)
- Modify: `portal-dal/.../gateway/GatewayConfig.java` (添加 apisixConfig)
- Modify: `portal-dal/.../entity/Gateway.java` (添加 apisixConfig 字段)
- Create: `portal-bootstrap/.../db/migration/V4__Add_apisix_gateway_support.sql`
- Create: `portal-server/src/test/.../ApisixConfigTest.java`

---

### Phase 1 Task 1.2: 实现 ApisixClient

**状态**: 🔄 进行中

#### Task 1.2.1: ApisixClient 基础结构 ✅

**状态**: ✅ 已完成

**测试验证**:
```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
mvn test -pl portal-server -Dtest=ApisixClientTest
# Tests run: 4, Failures: 0, Errors: 0, Skipped: 0 - BUILD SUCCESS
```

**变更文件**:
- Create: `portal-server/.../gateway/client/ApisixClient.java`
- Create: `portal-server/src/test/.../ApisixClientTest.java`

#### Task 1.2.2: ApisixClient HTTP 请求能力 ✅

**状态**: ✅ 已完成

**测试验证**:
```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
mvn test -pl portal-server -Dtest=ApisixClientTest
# Tests run: 9, Failures: 0, Errors: 0, Skipped: 0 - BUILD SUCCESS
```

**新增方法**:
- `buildUrl(String path)` - 构建完整 API URL
- `buildHeaders()` - 构建带 X-API-KEY 的请求头

#### Task 1.2.3: ApisixClient execute 方法 ✅

**状态**: ✅ 已完成

**测试验证**:
```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
mvn test -pl portal-server -Dtest=ApisixClientTest
# Tests run: 13, Failures: 0, Errors: 0, Skipped: 0 - BUILD SUCCESS
```

**新增方法**:
- `execute(path, method, queryParams, body, responseType)` - 执行 HTTP 请求
- `buildUrl(path, queryParams)` - 构建带查询参数的 URL

---

### Phase 1 Task 1.3: 实现 ApisixOperator

**状态**: ✅ 已完成

**测试验证**:
```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
mvn test -pl portal-server -Dtest="Apisix*Test"
# Tests run: 32, Failures: 0, Errors: 0, Skipped: 0 - BUILD SUCCESS
```

**变更文件**:
- Create: `portal-server/.../gateway/ApisixOperator.java`
- Create: `portal-server/src/test/.../ApisixOperatorTest.java`
- Modify: `portal-server/.../gateway/GatewayOperator.java` (添加 APISIX 支持)

**实现内容**:
- ApisixOperator 继承 GatewayOperator<ApisixClient>
- 使用 @Service 注解支持 Spring 自动发现
- getGatewayType() 返回 GatewayType.APISIX
- 初期方法抛出 UnsupportedOperationException（待 Phase 2/3 实现）
- GatewayOperator.createClient() 添加 APISIX case
- GatewayOperator.buildClientKey() 支持 APISIX 配置

---

### Phase 2 Task 2.1: 实现 mcp-bridge 配置

**状态**: ✅ 已完成

**测试验证**:
```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
mvn test -pl portal-server -Dtest="Apisix*Test"
# Tests run: 42, Failures: 0, Errors: 0, Skipped: 0 - BUILD SUCCESS
```

**变更文件**:
- Create: `portal-server/.../gateway/model/ApisixRoute.java`
- Create: `portal-dal/.../result/mcp/ApisixMCPServerResult.java`
- Create: `portal-server/src/test/.../ApisixMcpTest.java`
- Modify: `portal-server/.../gateway/client/ApisixClient.java` (添加 Route API)
- Modify: `portal-server/.../gateway/ApisixOperator.java` (实现 fetchMcpServers)
- Modify: `portal-dal/.../result/mcp/GatewayMCPServerResult.java` (添加 ApisixMCPServerResult)

**实现内容**:
- ApisixRoute 模型类表示 APISIX Route 对象
- ApisixMCPServerResult 继承 GatewayMCPServerResult
- ApisixClient 添加 listRoutes/getRoute/createRoute/updateRoute/deleteRoute 方法
- fetchMcpServers 实现从 Route 列表中筛选 mcp-bridge 插件

---

### Phase 2 Task 2.2: MCP Server CRUD

**状态**: ✅ 已完成

**测试验证**:
```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
mvn test -pl portal-server -Dtest="Apisix*Test"
# Tests run: 46, Failures: 0, Errors: 0, Skipped: 0 - BUILD SUCCESS
```

**变更文件**:
- Create: `portal-dal/.../product/ApisixRefConfig.java`
- Create: `portal-server/src/test/.../ApisixMcpConfigTest.java`
- Modify: `portal-server/.../gateway/ApisixOperator.java` (实现 fetchMcpConfig)

**实现内容**:
- ApisixRefConfig 配置类（routeId, mcpServerName, modelRouteName）
- fetchMcpConfig 实现：从 Route 获取 mcp-bridge 配置并转换为 MCPConfigResult

---

### Phase 2 Task 2.3: Consumer 管理

**状态**: ⏳ 待开始

