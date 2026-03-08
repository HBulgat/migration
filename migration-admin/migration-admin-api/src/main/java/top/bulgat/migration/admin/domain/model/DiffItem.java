package top.bulgat.migration.admin.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import top.bulgat.migration.config.common.model.enums.DiffType;

/**
 * 管理端查询接口展示的 Diff 明细项。
 */
@Getter
@AllArgsConstructor
public class DiffItem {

    private final String fieldPath;
    private final String oldValue;
    private final String newValue;
    private final DiffType diffType;
}
