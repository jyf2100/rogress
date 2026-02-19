package com.alibaba.apiopenplatform.dto.result.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author zh
 */
@Data
@Schema(
        oneOf = {
                HigressModelResult.class,
                AIGWModelAPIResult.class,
                ApisixModelResult.class,
        },
        discriminatorProperty = "type"
)
public class GatewayModelAPIResult {
}
