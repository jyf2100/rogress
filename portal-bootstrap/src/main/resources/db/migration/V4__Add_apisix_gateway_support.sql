-- V4__Add_apisix_gateway_support.sql
-- Add apisix_config column to gateway table for APISIX gateway support
-- Description: Support for Apache APISIX gateway integration with mcp-bridge plugin

START TRANSACTION;

-- ========================================
-- Add apisix_config column to gateway table
-- ========================================
ALTER TABLE `gateway`
ADD COLUMN `apisix_config` json DEFAULT NULL
AFTER `apsara_gateway_config`;

COMMIT;
