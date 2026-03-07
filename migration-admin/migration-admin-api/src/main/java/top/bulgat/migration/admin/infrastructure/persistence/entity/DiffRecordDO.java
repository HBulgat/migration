package top.bulgat.migration.admin.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * Diff 记录持久化对象。
 */
@Data
@TableName("diff_record")
public class DiffRecordDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("migration_key")
    private String migrationKey;
    @TableField("trace_id")
    private String traceId;
    @TableField("old_response")
    private String oldResponse;
    @TableField("new_response")
    private String newResponse;
    @TableField("diff_results")
    private String diffResults;
    @TableField("has_diff")
    private Integer hasDiff;
    @TableField("diff_type")
    private String diffType;
    @TableField("grayscale_param")
    private String grayscaleParam;
    @TableField("old_cost_time_ms")
    private Integer oldCostTimeMs;
    @TableField("new_cost_time_ms")
    private Integer newCostTimeMs;
    @TableField("total_cost_time_ms")
    private Integer totalCostTimeMs;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("old_success")
    private Integer oldSuccess;
    @TableField("new_success")
    private Integer newSuccess;
    @TableField("old_error_message")
    private String oldErrorMessage;
    @TableField("new_error_message")
    private String newErrorMessage;
    @TableField("old_request_params")
    private String oldRequestParams;
    @TableField("new_request_params")
    private String newRequestParams;
    @TableField("migration_status")
    private Integer migrationStatus;
    @TableField("grayscale_rules")
    private String grayscaleRules;
    @TableField("grayscale_hit")
    private Integer grayscaleHit;
    @TableField("fallback_triggered")
    private Integer fallbackTriggered;
}
