DROP DATABASE IF EXISTS migration;
CREATE DATABASE IF NOT EXISTS migration DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE migration;
CREATE TABLE IF NOT EXISTS diff_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    migration_key VARCHAR(64) NOT NULL COMMENT '迁移任务key',
    trace_id VARCHAR(64) COMMENT '链路ID',
    old_response LONGTEXT COMMENT '旧接口响应',
    new_response LONGTEXT COMMENT '新接口响应',
    diff_results LONGTEXT COMMENT 'Diff结果(JSON)',
    has_diff TINYINT NOT NULL DEFAULT 0 COMMENT '是否有差异(0-否/1-是)',
    diff_type VARCHAR(32) COMMENT '差异类型',
    gray_param VARCHAR(512) COMMENT '灰度参数',
    old_cost_time_ms INT COMMENT '旧接口耗时(ms)',
    new_cost_time_ms INT COMMENT '新接口耗时(ms)',
    total_cost_time_ms INT COMMENT '总耗时(ms)',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    old_success TINYINT DEFAULT NULL COMMENT '旧接口是否调用成功(0-否/1-是)',
    new_success TINYINT DEFAULT NULL COMMENT '新接口是否调用成功(0-否/1-是)',
    old_error_message TEXT DEFAULT NULL COMMENT '旧接口异常信息',
    new_error_message TEXT DEFAULT NULL COMMENT '新接口异常信息',
    old_request_params LONGTEXT DEFAULT NULL COMMENT '旧接口请求入参(JSON)',
    new_request_params LONGTEXT DEFAULT NULL COMMENT '新接口请求入参(JSON)',
    migration_status INT DEFAULT NULL COMMENT '当前迁移阶段(1-7)',
    gray_rules LONGTEXT DEFAULT NULL COMMENT '灰度规则(JSON)',
    gray_hit TINYINT DEFAULT NULL COMMENT '是否命中灰度规则(0-否/1-是)',
    fallback_triggered TINYINT DEFAULT NULL COMMENT '是否触发降级(0-否/1-是)',
    KEY idx_migration_key (migration_key),
    KEY idx_trace_id (trace_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Diff结果记录表';
#
# -- 增量迁移语句(已有表执行)
# ALTER TABLE diff_record
#   ADD COLUMN old_success TINYINT DEFAULT NULL COMMENT '旧接口是否调用成功(0-否/1-是)',
#   ADD COLUMN new_success TINYINT DEFAULT NULL COMMENT '新接口是否调用成功(0-否/1-是)',
#   ADD COLUMN old_error_message TEXT DEFAULT NULL COMMENT '旧接口异常信息',
#   ADD COLUMN new_error_message TEXT DEFAULT NULL COMMENT '新接口异常信息',
#   ADD COLUMN old_request_params LONGTEXT DEFAULT NULL COMMENT '旧接口请求入参(JSON)',
#   ADD COLUMN new_request_params LONGTEXT DEFAULT NULL COMMENT '新接口请求入参(JSON)',
#   ADD COLUMN migration_status INT DEFAULT NULL COMMENT '当前迁移阶段(1-7)',
#   ADD COLUMN gray_rules LONGTEXT DEFAULT NULL COMMENT '灰度规则(JSON)',
#   ADD COLUMN gray_hit TINYINT DEFAULT NULL COMMENT '是否命中灰度规则(0-否/1-是)',
#   ADD COLUMN fallback_triggered TINYINT DEFAULT NULL COMMENT '是否触发降级(0-否/1-是)';
