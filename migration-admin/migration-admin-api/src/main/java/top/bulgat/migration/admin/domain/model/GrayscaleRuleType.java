package top.bulgat.migration.admin.domain.model;

/**
 * Grayscale rule type module.
 */
public enum GrayscaleRuleType {
    PERCENTAGE,
    BLACKLIST,
    WHITELIST,
    EXPRESSION;

    /**
     * Parse rule type from text module.
     */
    public static GrayscaleRuleType fromValue(String value) {
        return GrayscaleRuleType.valueOf(value.toUpperCase());
    }
}
