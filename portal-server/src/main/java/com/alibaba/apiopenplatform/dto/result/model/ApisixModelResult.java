package com.alibaba.apiopenplatform.dto.result.model;

import com.alibaba.apiopenplatform.dto.converter.OutputConverter;
import com.alibaba.apiopenplatform.service.gateway.model.ApisixRoute;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * APISIX 模型 API 结果
 *
 * 表示通过 ai-proxy 插件配置的 AI 模型路由
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ApisixModelResult extends GatewayModelAPIResult
        implements OutputConverter<ApisixModelResult, ApisixRoute> {

    /**
     * 路由 ID
     */
    private String routeId;

    /**
     * 模型路由名称
     */
    private String modelRouteName;

    /**
     * 路由 URI
     */
    private String uri;

    /**
     * 模型提供商 (openai, azure, etc.)
     */
    private String provider;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 是否启用
     */
    private Boolean enabled;

    @Override
    public ApisixModelResult convertFrom(ApisixRoute route) {
        this.setRouteId(route.getId());
        this.setUri(route.getUri());
        this.setEnabled(route.isEnabled());

        // 使用路由名称作为模型路由名称
        String name = route.getName();
        if (name == null || name.isEmpty()) {
            name = route.getId();
        }
        this.setModelRouteName(name);

        // 解析 ai-proxy 插件配置
        if (route.hasAiProxyPlugin()) {
            this.setProvider(route.getAiProxyProvider());
            this.setModelName(route.getAiProxyModelName());
        }

        return this;
    }
}
