package top.bulgat.migration.diff.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * Diff record data object for MyBatis module.
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
}
