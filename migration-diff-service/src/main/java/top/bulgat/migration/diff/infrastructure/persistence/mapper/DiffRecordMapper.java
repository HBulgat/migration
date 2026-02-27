package top.bulgat.migration.diff.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.bulgat.migration.diff.infrastructure.persistence.entity.DiffRecordDO;

/**
 * DiffRecordMapper is the MyBatis mapper.
 */
@Mapper
public interface DiffRecordMapper extends BaseMapper<DiffRecordDO> {
}

