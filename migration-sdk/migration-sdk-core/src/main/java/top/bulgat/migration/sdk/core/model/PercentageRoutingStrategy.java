package top.bulgat.migration.sdk.core.model;

/**
 * 百分比路由策略常量。
 */
public class PercentageRoutingStrategy {
    /** 基于业务标识哈希的稳定分流策略。 */
    public static final String HASH = "HASH";
    /** 纯随机分流策略。 */
    public static final String RANDOM = "RANDOM";
}
