# APISIX P1/P2：订阅级访问控制（可撤销）+ 测试固化

## 目标
在 APISIX 网关场景下，把订阅授权从“仅开启 key-auth”升级为“**订阅级可控**”：

- **P1**：对每个 Product 对应的 APISIX Route，使用 `consumer-restriction` 插件维护白名单，实现“订阅才可访问”。
- **P1**：实现 revoke（取消订阅/删除 Consumer）时的权限回收，避免“退订仍可访问”。
- **P2**：用单测固化 Route 插件合并/回收行为（必要时补一个可选集成测）。

## 现状（Facts）
- `ApisixOperator.authorizeConsumer()` 当前仅确保 Route 上存在 `key-auth` 插件，**不限制消费者**，导致任意持 key 的 Consumer 可能访问所有开启 key-auth 的 Route。
- `ApisixOperator.revokeConsumerAuthorization()` 目前只打日志，**不会回收订阅授权**。
- `ConsumerAuthConfig` 没有 APISIX 专用字段，revoke 阶段无法定位需要修改的 Route。

## 方案（Design）
### 方案 A（推荐）：Route 上启用 `consumer-restriction`（whitelist by consumer_name）
- 在 `authorizeConsumer()` 中：
  1) 获取 Route
  2) 合并/开启 `key-auth`
  3) 合并 `consumer-restriction` 插件配置：`type=consumer_name`，并把 `consumerId` 加入 `whitelist`
  4) 更新 Route
  5) 返回 `ConsumerAuthConfig.apisixAuthConfig.routeId`，用于 revoke
- 在 `revokeConsumerAuthorization()` 中：
  - 根据 `authConfig.apisixAuthConfig.routeId` 获取 Route
  - 从 `consumer-restriction.whitelist` 移除 `consumerId`
  - 若 whitelist 为空则移除整个 `consumer-restriction` 插件

**优点**：不引入额外 APISIX 资源（consumer group），实现简单、可解释、与 Higress 的“允许列表”语义一致。  
**缺点**：whitelist 可能变长；并发更新同一 Route 仍可能有“最后写入覆盖”风险（后续可做 P2.2 优化）。

### 方案 B：按 Product 创建 Consumer Group（type=consumer_group_id）
需要为每个 Product 管理 consumer group 生命周期与 membership，工程量更大，本轮不做。

## 范围（Scope）
- ✅ APISIX：Route 插件 `consumer-restriction` 白名单授权与回收。
- ✅ `ConsumerAuthConfig` 增加 `ApisixAuthConfig`（JSON 存储，兼容旧数据）。
- ✅ 单元测试：授权合并、撤销回收（不依赖真实 APISIX）。
- ⛔ 不做：Consumer Group、Dashboard、分页/重试增强、APISIX 上游/路由创建策略调整。

## 验收标准（Acceptance / DoD）
- authorize 后：Route plugins 包含
  - `key-auth`（存在即可）
  - `consumer-restriction.whitelist` 包含当前 `consumerId`
- revoke 后：Route plugins 中该 `consumerId` 被移除；
  - 若 whitelist 为空：移除 `consumer-restriction` 插件（避免空 whitelist 的“默认拒绝所有”风险）。
- `portal-server` 相关单测通过。

## 任务清单（TDD）
### Task 1：新增 ApisixAuthConfig 并接入 ConsumerAuthConfig
**Files**
- Add: `portal-dal/src/main/java/com/alibaba/apiopenplatform/support/consumer/ApisixAuthConfig.java`
- Modify: `portal-dal/src/main/java/com/alibaba/apiopenplatform/support/consumer/ConsumerAuthConfig.java`
- Test: `portal-dal` 可选（若需要验证 JSON 兼容性再补）

### Task 2：实现 APISIX 授权（consumer-restriction whitelist）
**Files**
- Modify: `portal-server/src/main/java/com/alibaba/apiopenplatform/service/gateway/ApisixOperator.java`
- Test: `portal-server/src/test/java/com/alibaba/apiopenplatform/service/gateway/ApisixConsumerRestrictionAuthTest.java`

### Task 3：实现 APISIX revoke（从 whitelist 回收）
**Files**
- Modify: `portal-server/src/main/java/com/alibaba/apiopenplatform/service/gateway/ApisixOperator.java`
- Test: 复用 `ApisixConsumerRestrictionAuthTest`

## 验证命令
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
mvn test -pl portal-server -am -Dtest='ApisixConsumerRestrictionAuthTest' -Dsurefire.failIfNoSpecifiedTests=false
mvn test -pl portal-server
```
