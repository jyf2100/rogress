# APISIX P0 修复：订阅授权主链路 + MCP 配置可用性

## 目标
把 APISIX 网关在 “订阅 → 创建/更新 Consumer → 授权 → 下发 MCP Config/Tools 调用” 的主链路补齐到可用状态，并修复会导致误重建 Consumer、MCP URL 无法使用的 P0 问题。

## 范围（Scope）
- ✅ 修复 `GatewayServiceImpl` 在 APISIX 场景下的 `authorizeConsumer()` refConfig 选择与 `getGatewayConfig()` 配置回填。
- ✅ 修复 `ProductRefResult` 缺失 `ApisixRefConfig` 导致无法授权的问题。
- ✅ 修复 MCP 配置域名占位符 `<apisix-gateway-ip>` 不会被替换的问题。
- ✅ 规范化 APISIX Route `uri`（如 `/*`）到可用的 MCP base path，避免拼接出 `/*/sse`。
- ✅ 修复 APISIX Consumer 存在性检查在网络异常时“误判为不存在→重复创建”的风险。

非目标（Not Goals）
- ❌ 本轮不做 APISIX “订阅级白名单授权”（例如 `consumer-restriction` 插件）。
- ❌ 本轮不做 Admin API 分页/超时可配置等增强（属于 P1/P2）。

## 验收标准（Acceptance / DoD）
- `GatewayServiceImpl.authorizeConsumer()` 在 `GatewayType.APISIX` 时，会把 `ProductRefResult.apisixRefConfig` 传给 `ApisixOperator.authorizeConsumer()`。
- `GatewayServiceImpl.getGatewayConfig()` 会返回包含 `apisixConfig` 的 `GatewayConfig`。
- `ProductServiceImpl.getProductRef()` 返回的 `ProductRefResult` 包含 `apisixRefConfig`（能被后续授权使用）。
- `MCPConfigResult.convertDomainToGatewayIp()` 同时支持替换 `<higress-gateway-ip>` 与 `<apisix-gateway-ip>`。
- `ApisixOperator.fetchMcpConfig()` 输出的 `mcpServerConfig.path` 不包含 `*`（例如 `/mcp/filesystem`），并且 `toStandardMcpServer()` 拼出来的 URL 不出现 `/*/sse`。
- `ApisixClient.consumerExists()` 仅在 404 时返回 `false`；对网络/认证等异常抛出，让上层按“保守不重建”的策略处理。

## 任务清单（TDD）

### Task 1: ProductRefResult 补齐 ApisixRefConfig
**Files**
- Modify: `portal-server/src/main/java/com/alibaba/apiopenplatform/dto/result/product/ProductRefResult.java`
- Test: `portal-server/src/test/java/com/alibaba/apiopenplatform/dto/result/product/ProductRefResultTest.java`

### Task 2: GatewayServiceImpl APISIX refConfig + config 回填
**Files**
- Modify: `portal-server/src/main/java/com/alibaba/apiopenplatform/service/impl/GatewayServiceImpl.java`
- Test: `portal-server/src/test/java/com/alibaba/apiopenplatform/service/impl/GatewayServiceImplApisixP0Test.java`

### Task 3: MCP placeholder 替换支持 APISIX
**Files**
- Modify: `portal-server/src/main/java/com/alibaba/apiopenplatform/dto/result/mcp/MCPConfigResult.java`
- Test: `portal-server/src/test/java/com/alibaba/apiopenplatform/dto/result/mcp/MCPConfigResultTest.java`

### Task 4: fetchMcpConfig path 规范化
**Files**
- Modify: `portal-server/src/main/java/com/alibaba/apiopenplatform/service/gateway/ApisixOperator.java`
- Test: `portal-server/src/test/java/com/alibaba/apiopenplatform/service/gateway/ApisixMcpConfigPathTest.java`

### Task 5: consumerExists 网络异常不误判
**Files**
- Modify: `portal-server/src/main/java/com/alibaba/apiopenplatform/service/gateway/client/ApisixClient.java`
- Test: `portal-server/src/test/java/com/alibaba/apiopenplatform/service/gateway/client/ApisixClientConsumerExistsTest.java`

## 验证命令
```bash
# 确保 Maven 使用 JDK 17（本项目以 Java 17 为基线）
# macOS:
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
mvn -v | head -n 6

mvn test -pl portal-server -Dtest='*Apisix*Test'
mvn test -pl portal-server -Dtest='GatewayServiceImplApisixP0Test'
mvn test -pl portal-server -Dtest='MCPConfigResultTest,ProductRefResultTest'
```
