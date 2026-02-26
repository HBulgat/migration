package top.bulgat.migration.admin.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Diff item displayed by admin query APIs module.
 */
@Getter
@AllArgsConstructor
public class DiffItem {

    private final String fieldPath;
    private final String oldValue;
    private final String newValue;
    private final DiffType diffType;
}
