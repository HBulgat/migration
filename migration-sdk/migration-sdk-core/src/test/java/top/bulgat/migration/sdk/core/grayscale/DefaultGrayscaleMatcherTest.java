package top.bulgat.migration.sdk.core.grayscale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import top.bulgat.migration.sdk.core.model.GrayscaleConfig;

class DefaultGrayscaleMatcherTest {

    private final DefaultGrayscaleMatcher matcher = new DefaultGrayscaleMatcher();

    @Test
    void match_shouldHitWhitelist() {
        GrayscaleConfig rule = GrayscaleConfig.builder()
                .ruleType("WHITELIST")
                .ruleValue("[\"1001\",\"1002\"]")
                .enable(true)
                .build();

        assertTrue(matcher.match(List.of(rule), Map.of("userId", "1001")));
        assertFalse(matcher.match(List.of(rule), Map.of("userId", "1003")));
    }

    @Test
    void match_shouldHitPercentageWhenHundred() {
        GrayscaleConfig rule = GrayscaleConfig.builder()
                .ruleType("PERCENTAGE")
                .ruleValue("100")
                .enable(true)
                .build();

        assertTrue(matcher.match(List.of(rule), Map.of("userId", "any-user")));
    }

    @Test
    void match_shouldEvaluateExpression() {
        GrayscaleConfig rule = GrayscaleConfig.builder()
                .ruleType("EXPRESSION")
                .ruleValue("#userId == '1001' and #level >= 3")
                .enable(true)
                .build();

        assertTrue(matcher.match(List.of(rule), Map.of("userId", "1001", "level", 3)));
        assertFalse(matcher.match(List.of(rule), Map.of("userId", "1002", "level", 3)));
    }
}
