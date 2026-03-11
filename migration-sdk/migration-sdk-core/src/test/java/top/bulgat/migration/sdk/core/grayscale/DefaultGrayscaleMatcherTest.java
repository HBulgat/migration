package top.bulgat.migration.sdk.core.grayscale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import top.bulgat.migration.sdk.core.model.GrayscaleConfig;
import top.bulgat.migration.sdk.core.model.PercentageRoutingStrategy;

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
                .ruleValue("userId == '1001' && level >= `3`")
                .enable(true)
                .build();

        assertTrue(matcher.match(List.of(rule), Map.of("userId", "1001", "level", 3)));
        assertFalse(matcher.match(List.of(rule), Map.of("userId", "1002", "level", 3)));
    }

//    @Test
    void match_shouldHitPercentageRandomly() {
        // Create matcher with RANDOM strategy
        DefaultGrayscaleMatcher randomMatcher = new DefaultGrayscaleMatcher();

        GrayscaleConfig rule = GrayscaleConfig.builder()
                .ruleType("PERCENTAGE")
                .ruleValue("50")
                .enable(true)
                .build();

        int hits = 0;
        int total = 1000;
        Map<String, Object> params = Map.of("userId", "stable-id");
        for (int i = 0; i < total; i++) {
            if (randomMatcher.match(List.of(rule), params)) {
                hits++;
            }
        }

        // Probabilistic check for 50%
        assertTrue(hits > 0 && hits < total, "Random strategy should distribute hits. Actual hits: " + hits);
    }
}
