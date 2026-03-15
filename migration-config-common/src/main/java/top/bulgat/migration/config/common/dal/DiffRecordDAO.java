package top.bulgat.migration.config.common.dal;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.bulgat.migration.config.common.model.dataobject.DiffRecordDO;

/**
 * DiffRecord 的 MyBatis Mapper。
 */
@Mapper
public interface DiffRecordDAO extends BaseMapper<DiffRecordDO> {

    @Select("<script>" +
            "SELECT " +
            "  FROM_UNIXTIME(FLOOR(UNIX_TIMESTAMP(create_time) / #{granularitySeconds}) * #{granularitySeconds}) as timePoint, " +
            "  COUNT(*) as totalCount, " +
            "  SUM(CASE WHEN has_diff = 1 THEN 1 ELSE 0 END) as diffCount, " +
            "  IFNULL(AVG(old_cost_time_ms), 0) as avgOldCost, " +
            "  IFNULL(AVG(new_cost_time_ms), 0) as avgNewCost " +
            "FROM diff_record " +
            "WHERE migration_key = #{key} " +
            "  AND create_time &gt;= #{start} " +
            "  AND create_time &lt;= #{end} " +
            "  <if test='status != null'> AND migration_status = #{status} </if> " +
            "GROUP BY timePoint " +
            "ORDER BY timePoint ASC" +
            "</script>")
    List<DiffStatisticsQueryResult> selectTrendStatistics(
            @Param("key") String key,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("status") Integer status,
            @Param("granularitySeconds") int granularitySeconds);
}
