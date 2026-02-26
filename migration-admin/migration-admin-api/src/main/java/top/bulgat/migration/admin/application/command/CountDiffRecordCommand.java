package top.bulgat.migration.admin.application.command;

import java.time.LocalDate;

/**
 * CountDiffRecordCommand contains filters for diff record count statistics.
 */
public record CountDiffRecordCommand(
        String migrationKey,
        Integer hasDiff,
        LocalDate startDate,
        LocalDate endDate) {
}
