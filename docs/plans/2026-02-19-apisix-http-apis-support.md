# APISIX HTTP/REST APIs 管理支持

## 目标
让 REST API 产品可以关联 APISIX 网关的 HTTP Route。

## 分析

### 现状
- `ApisixOperator.fetchHTTPAPIs()` 抛出 UnsupportedOperationException
- `ApisixOperator.fetchRESTAPIs()` 抛出 UnsupportedOperationException
- `ApisixOperator.fetchAPIConfig()` 抛出 UnsupportedOperationException

### 方案
基于 Route 过滤：
1. 获取所有 Route
2. 排除带有 mcp-bridge 或 ai-proxy 插件的 Route
3. 剩余 Route 视为 HTTP API

---

## 任务清单

### Task 1: 创建 HTTP API 结果模型
**Files**:
- Create: `portal-server/src/main/java/com/alibaba/apiopenplatform/dto/result/httpapi/ApisixHttpApiResult.java`

**Step 1**: 创建测试
```java
// ApisixHttpApiResultTest.java
@Test
void shouldConvertFromRoute() {
    ApisixRoute route = new ApisixRoute();
    route.setId("test-route");
    route.setName("Test API");
    route.setUri("/api/test/*");
    route.setMethods(List.of("GET", "POST"));
    route.setStatus(true);

    ApisixHttpApiResult result = new ApisixHttpApiResult().convertFrom(route);

    assertEquals("test-route", result.getApiId());
    assertEquals("Test API", result.getApiName());
    assertTrue(result.getEnabled());
}
```

**Step 2**: 运行测试 → 红（类不存在）

**Step 3**: 创建实现
```java
@Data
public class ApisixHttpApiResult extends APIResult
        implements OutputConverter<ApisixHttpApiResult, ApisixRoute> {

    private String routeId;
    private String uri;
    private List<String> methods;
    private Boolean enabled;

    @Override
    public ApisixHttpApiResult convertFrom(ApisixRoute route) {
        this.setApiId(route.getId());
        this.setApiName(route.getName() != null ? route.getName() : route.getId());
        this.setRouteId(route.getId());
        this.setUri(route.getUri());
        this.setMethods(route.getMethods());
        this.setEnabled(route.isEnabled());
        return this;
    }
}
```

**Step 4**: 运行测试 → 绿

---

### Task 2: 实现 fetchHTTPAPIs
**Files**:
- Modify: `portal-server/src/main/java/com/alibaba/apiopenplatform/service/gateway/ApisixOperator.java`

**Step 1**: 创建测试
```java
@Test
void shouldFetchHttpApisExcludingMcpAndModelRoutes() {
    // Given
    ApisixRoute httpRoute = createRoute("http-route", null, null); // no special plugins
    ApisixRoute mcpRoute = createRoute("mcp-route", Map.of("mcp-bridge", Map.of()), null);
    ApisixRoute modelRoute = createRoute("model-route", null, Map.of("ai-proxy", Map.of()));

    when(client.listRoutes()).thenReturn(List.of(httpRoute, mcpRoute, modelRoute));

    // When
    PageResult<APIResult> result = operator.fetchHTTPAPIs(gateway, 1, 10);

    // Then
    assertEquals(1, result.getContent().size());
    assertEquals("http-route", result.getContent().get(0).getApiId());
}
```

**Step 2**: 运行测试 → 红

**Step 3**: 实现
```java
@Override
public PageResult<APIResult> fetchHTTPAPIs(Gateway gateway, int page, int size) {
    ApisixClient client = getClient(gateway);
    List<ApisixRoute> routes = client.listRoutes();

    List<APIResult> httpApis = routes.stream()
            .filter(route -> !route.hasMcpBridgePlugin() && !route.hasAiProxyPlugin())
            .map(route -> new ApisixHttpApiResult().convertFrom(route))
            .collect(Collectors.toList());

    // Pagination
    int total = httpApis.size();
    int fromIndex = (page - 1) * size;
    int toIndex = Math.min(fromIndex + size, total);

    if (fromIndex >= total) {
        return PageResult.of(Collections.emptyList(), page, size, total);
    }

    return PageResult.of(httpApis.subList(fromIndex, toIndex), page, size, total);
}
```

**Step 4**: 运行测试 → 绿

---

### Task 3: 实现 fetchRESTAPIs
**Files**:
- Modify: `portal-server/src/main/java/com/alibaba/apiopenplatform/service/gateway/ApisixOperator.java`

**Step 1**: 测试（与 fetchHTTPAPIs 相同逻辑）
**Step 2**: 实现（调用 fetchHTTPAPIs）

```java
@Override
public PageResult<APIResult> fetchRESTAPIs(Gateway gateway, int page, int size) {
    return fetchHTTPAPIs(gateway, page, size);
}
```

---

### Task 4: 实现 fetchAPIConfig
**Files**:
- Modify: `portal-server/src/main/java/com/alibaba/apiopenplatform/service/gateway/ApisixOperator.java`

**Step 1**: 创建测试
```java
@Test
void shouldFetchApiConfig() {
    ApisixRoute route = new ApisixRoute();
    route.setId("test-route");
    route.setUri("/api/test/*");

    when(client.getRoute("test-route")).thenReturn(route);

    ApisixRefConfig refConfig = new ApisixRefConfig();
    refConfig.setRouteId("test-route");

    String config = operator.fetchAPIConfig(gateway, refConfig);

    assertNotNull(config);
    assertTrue(config.contains("/api/test/*"));
}
```

**Step 2**: 运行测试 → 红

**Step 3**: 实现
```java
@Override
public String fetchAPIConfig(Gateway gateway, Object config) {
    ApisixRefConfig refConfig = (ApisixRefConfig) config;
    ApisixClient client = getClient(gateway);

    ApisixRoute route = client.getRoute(refConfig.getRouteId());
    if (route == null) {
        throw new RuntimeException("Route not found: " + refConfig.getRouteId());
    }

    // Build API config result
    ApiConfigResult result = new ApiConfigResult();
    result.setApiId(route.getId());
    result.setApiName(route.getName() != null ? route.getName() : route.getId());

    // Build route config
    ApiConfigResult.ApiRouteConfig routeConfig = new ApiConfigResult.ApiRouteConfig();
    routeConfig.setPath(route.getUri());
    routeConfig.setMethods(route.getMethods());
    result.setRouteConfig(routeConfig);

    return JSONUtil.toJsonStr(result);
}
```

**Step 4**: 运行测试 → 绿

---

### Task 5: 前端支持（可选）
**Files**:
- Modify: `portal-web/api-portal-admin/src/components/api-product/ApiProductLinkApi.tsx`

添加 APISIX 网关的 HTTP API 获取逻辑。

---

## 验证

```bash
# 编译
mvn clean compile -q

# 运行 APISIX 相关测试
mvn test -Dtest="*Apisix*" -pl portal-server

# 手动验证
curl -s "http://localhost:8081/gateways/{gatewayId}/http-apis" -H "Authorization: Bearer $TOKEN"
```

---

## 提交

```bash
git add -A
git commit -m "feat(gateway): add APISIX HTTP/REST APIs support"
git push
```
