package top.bulgat.migration.admin.domain.service;

import org.springframework.stereotype.Service;
import top.bulgat.migration.admin.domain.model.DiffRuleType;

/**
 * Diff规则领域服务
 */
@Service
public class DiffRuleDomainService {

    public void validateRule(DiffRuleType ruleType, String fieldPath, String ruleValue) {
        if (ruleType == null) {
            throw new IllegalArgumentException("ruleType cannot be null");
        }
        if (fieldPath == null || fieldPath.trim().isEmpty()) {
            throw new IllegalArgumentException("fieldPath cannot be empty");
        }

        switch (ruleType) {
            case IGNORE:
                // IGNORE可以没有ruleValue
                break;
            case TOLERANCE:
                if (ruleValue == null || ruleValue.trim().isEmpty()) {
                    throw new IllegalArgumentException("TOLERANCE ruleValue cannot be empty");
                }
                try {
                    Double.parseDouble(ruleValue);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("TOLERANCE ruleValue must be a valid number");
                }
                break;
            case SCRIPT:
            case SORT:
                if (ruleValue == null || ruleValue.trim().isEmpty()) {
                    throw new IllegalArgumentException(ruleType.name() + " ruleValue cannot be empty");
                }
                break;
        }
    }
}
