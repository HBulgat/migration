package top.bulgat.migration.diff.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Single diff detail item module.
 */
@Getter
@AllArgsConstructor
public class DiffItem {

    private final String fieldPath;
    private final String oldValue;
    private final String newValue;
    private final DiffType diffType;
}
