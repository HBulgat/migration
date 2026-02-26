package top.bulgat.migration.admin.application.command;

import java.time.LocalDate;

/**
 * StatisticsDiffRecordCommand contains filters for querying diff statistics.
 */
public record StatisticsDiffRecordCommand(
        String migrationKey,
        LocalDate startDate,
        LocalDate endDate) {
}
