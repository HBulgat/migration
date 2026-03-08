package top.bulgat.migration.config.common.dal;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.bulgat.migration.config.common.model.dataobject.DiffRecordDO;

/**
 * DiffRecord 的 MyBatis Mapper。
 */
@Mapper
public interface DiffRecordDAO extends BaseMapper<DiffRecordDO> {
}
