-- Runtime schema fixes. This patch is additive and safe to run repeatedly.

SET @bill_id_exists = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'fee_refund'
    AND column_name = 'bill_id'
);
SET @ddl = IF(
  @bill_id_exists = 0,
  'ALTER TABLE `fee_refund` ADD COLUMN `bill_id` bigint DEFAULT NULL COMMENT ''关联账单ID'' AFTER `deposit_id`',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @bill_id_index_exists = (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'fee_refund'
    AND index_name = 'idx_bill_id'
);
SET @ddl = IF(
  @bill_id_index_exists = 0,
  'ALTER TABLE `fee_refund` ADD INDEX `idx_bill_id` (`bill_id`)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
