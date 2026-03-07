package top.bulgat.migration.admin.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.bulgat.migration.admin.infrastructure.persistence.entity.DiffRecordDO;

/**
 * DiffRecord 的 MyBatis Mapper。
 */
@Mapper
public interface DiffRecordMapper extends BaseMapper<DiffRecordDO> {
}

