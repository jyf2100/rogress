-- V5__Add_apisix_ref_config.sql
-- Add apisix_ref_config column to product_ref table for APISIX gateway resource reference
-- Description: Support for referencing APISIX routes (MCP Server, Model routes) in products

START TRANSACTION;

-- ========================================
-- Add apisix_ref_config column to product_ref table
-- ========================================
ALTER TABLE `product_ref`
ADD COLUMN `apisix_ref_config` json DEFAULT NULL
AFTER `higress_ref_config`;

COMMIT;
