export interface Gateway {
  gatewayId: string
  gatewayName: string
  gatewayType: 'APIG_API' | 'HIGRESS' | 'APIG_AI' | 'ADP_AI_GATEWAY' | 'APSARA_GATEWAY' | 'APISIX'
  createAt: string
  apigConfig?: ApigConfig
  higressConfig?: HigressConfig
  apsaraGatewayConfig?: ApsaraGatewayConfig
  apisixConfig?: ApisixConfig
}

export interface ApigConfig {
  region: string
  accessKey: string
  secretKey: string
}

export interface HigressConfig {
  username: string
  address: string
  password: string
}

export interface ApsaraGatewayConfig {
  endpoint: string
  accessKey: string
  secretKey: string
  product: string
  version: string
  xAcsRoleId?: string
}

export interface ApisixConfig {
  adminApiEndpoint: string
  adminApiKey: string
  timeout?: number
}

export interface NacosInstance {
  nacosId: string
  nacosName: string
  serverUrl: string
  username: string
  password?: string
  accessKey?: string
  secretKey?: string
  description: string
  adminId: string
  createAt?: string | number
}

export type GatewayType = 'APIG_API' | 'APIG_AI' | 'HIGRESS' | 'ADP_AI_GATEWAY' | 'APSARA_GATEWAY' | 'APISIX'
