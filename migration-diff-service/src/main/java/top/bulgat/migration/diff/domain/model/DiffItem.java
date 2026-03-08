package top.bulgat.migration.diff.domain.model;

import lombok.AllArgsConstructor;

/**
 * 单个 Diff 明细项。
 */
public record DiffItem(String fieldPath, String oldValue, String newValue, DiffType diffType) {

}
