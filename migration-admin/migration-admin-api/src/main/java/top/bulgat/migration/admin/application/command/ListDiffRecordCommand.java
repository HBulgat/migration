package top.bulgat.migration.admin.application.command;

import java.time.LocalDate;

/**
 * ListDiffRecordCommand contains filters and pagination arguments for diff record listing.
 */
public record ListDiffRecordCommand(
        String migrationKey,
        Integer hasDiff,
        LocalDate startDate,
        LocalDate endDate,
        int page,
        int pageSize) {
}
