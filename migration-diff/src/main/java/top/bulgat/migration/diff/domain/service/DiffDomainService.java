package top.bulgat.migration.diff.domain.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.flipkart.zjsonpatch.JsonDiff;
import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import top.bulgat.common.exception.BizException;
import top.bulgat.common.exception.ErrorCode;
import top.bulgat.migration.diff.domain.model.DiffItem;
import top.bulgat.migration.diff.domain.model.DiffRequest;
import top.bulgat.migration.diff.domain.model.DiffResult;
import top.bulgat.migration.diff.domain.model.DiffRule;
import top.bulgat.migration.diff.domain.model.DiffRuleType;
import top.bulgat.migration.diff.domain.model.DiffType;
import top.bulgat.migration.diff.domain.rule.DiffRuleExecutor;
import top.bulgat.migration.diff.domain.rule.DiffRuleExecutorRegistry;

/**
 * Domain service for diff comparison.
 * Handles JSON patch generation, sort preprocessing, and rule filtering.
 */
@Component
public class DiffDomainService {

    private static final Logger log = LoggerFactory.getLogger(DiffDomainService.class);
    private static final Pattern TOKEN_PATTERN = Pattern.compile("^([\\w$-]+)(?:\\[(\\*|\\d+)])?$");

    private final ObjectMapper objectMapper;
    private final DiffRuleExecutorRegistry ruleExecutorRegistry;

    public DiffDomainService(ObjectMapper objectMapper, DiffRuleExecutorRegistry ruleExecutorRegistry) {
        this.objectMapper = objectMapper;
        this.ruleExecutorRegistry = ruleExecutorRegistry;
    }

    /**
     * Executes diff comparison for a single request.
     * Sort rules are applied before JSON patch calculation and rule filtering.
     *
     * @param request diff input payload
     * @param rules active rules for this migration key
     * @return diff result after rule filtering
     */
    public DiffResult execute(DiffRequest request, List<DiffRule> rules) {
        long start = System.currentTimeMillis();
        log.info("diff.domain start migrationKey={}, ruleCount={}", request.getMigrationKey(), rules.size());
        JsonNode oldNode = readJson(request.getOldJson());
        JsonNode newNode = readJson(request.getNewJson());

        applySortRules(oldNode, rules);
        applySortRules(newNode, rules);

        List<DiffItem> rawDiffItems = createPatchDiff(oldNode, newNode);
        List<DiffItem> filteredItems = applyRules(rawDiffItems, rules);
        long costTimeMs = System.currentTimeMillis() - start;
        log.info("diff.domain done migrationKey={}, rawDiffCount={}, filteredDiffCount={}, costTimeMs={}",
                request.getMigrationKey(), rawDiffItems.size(), filteredItems.size(), costTimeMs);
        return new DiffResult(!filteredItems.isEmpty(), filteredItems, costTimeMs);
    }

    private JsonNode readJson(String payload) {
        try {
            return objectMapper.readTree(payload);
        } catch (IOException ex) {
            log.debug("failed to parse diff json payload", ex);
            throw new BizException(ErrorCode.PARAM_ERROR, "invalid json payload");
        }
    }

    private void applySortRules(JsonNode rootNode, List<DiffRule> rules) {
        for (DiffRule rule : rules) {
            if (!rule.isEnable() || rule.getRuleType() != DiffRuleType.SORT) {
                continue;
            }
            sortArraysByRule(rootNode, normalizeRulePath(rule.getFieldPath()), rule.getRuleValue());
        }
    }

    private void sortArraysByRule(JsonNode rootNode, String arrayPath, String sortField) {
        if (arrayPath.isBlank()) {
            sortArrayNode(rootNode, sortField);
            return;
        }
        String[] tokens = arrayPath.split("\\.");
        traverseAndSort(rootNode, tokens, 0, sortField);
    }

    private void traverseAndSort(JsonNode current, String[] tokens, int index, String sortField) {
        if (current == null) {
            return;
        }
        if (index >= tokens.length) {
            sortArrayNode(current, sortField);
            return;
        }
        String token = tokens[index];
        Matcher matcher = TOKEN_PATTERN.matcher(token);
        if (!matcher.matches()) {
            return;
        }
        String field = matcher.group(1);
        String arrayFlag = matcher.group(2);
        JsonNode child = current.get(field);
        if (child == null) {
            return;
        }
        if (arrayFlag == null) {
            traverseAndSort(child, tokens, index + 1, sortField);
            return;
        }
        if (!(child instanceof ArrayNode arrayNode)) {
            return;
        }
        if ("*".equals(arrayFlag)) {
            for (JsonNode element : arrayNode) {
                traverseAndSort(element, tokens, index + 1, sortField);
            }
            return;
        }
        int arrayIndex = Integer.parseInt(arrayFlag);
        if (arrayIndex >= 0 && arrayIndex < arrayNode.size()) {
            traverseAndSort(arrayNode.get(arrayIndex), tokens, index + 1, sortField);
        }
    }

    private void sortArrayNode(JsonNode targetNode, String sortField) {
        if (!(targetNode instanceof ArrayNode arrayNode)) {
            return;
        }
        if (arrayNode.size() <= 1) {
            return;
        }
        List<JsonNode> elements = new ArrayList<>();
        arrayNode.forEach(elements::add);
        elements.sort(arrayComparator(sortField));
        arrayNode.removeAll();
        for (JsonNode element : elements) {
            arrayNode.add(element);
        }
    }

    private Comparator<JsonNode> arrayComparator(String sortField) {
        return Comparator.comparing(
                node -> extractSortKey(node, sortField),
                Comparator.nullsLast(this::compareSortValue));
    }

    private Comparable<?> extractSortKey(JsonNode node, String sortField) {
        if (node == null || sortField == null || sortField.isBlank()) {
            return null;
        }
        try {
            if (sortField.startsWith("$")) {
                Object value = JsonPath.read(node.toString(), sortField);
                return castComparable(value);
            }
            JsonNode cursor = node;
            for (String field : sortField.split("\\.")) {
                if (cursor == null) {
                    return null;
                }
                cursor = cursor.get(field);
            }
            return castComparable(jsonNodeToObject(cursor));
        } catch (Exception ex) {
            return null;
        }
    }

    private int compareSortValue(Comparable<?> left, Comparable<?> right) {
        if (left instanceof BigDecimal leftNum && right instanceof BigDecimal rightNum) {
            return leftNum.compareTo(rightNum);
        }
        return left.toString().compareTo(right.toString());
    }

    private Comparable<?> castComparable(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        if (value instanceof Comparable<?> comparable) {
            return comparable;
        }
        return value.toString();
    }

    private Object jsonNodeToObject(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.decimalValue();
        }
        if (node.isTextual()) {
            return node.textValue();
        }
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        return node.toString();
    }

    private List<DiffItem> createPatchDiff(JsonNode oldNode, JsonNode newNode) {
        JsonNode patch = JsonDiff.asJson(oldNode, newNode);
        List<DiffItem> items = new ArrayList<>();
        if (!patch.isArray()) {
            return items;
        }
        for (JsonNode patchNode : patch) {
            String op = patchNode.path("op").asText();
            String path = patchNode.path("path").asText();
            String from = patchNode.path("from").asText(null);
            DiffType diffType = convertOp(op);

            String oldValue = readNodeValue(oldNode, from != null ? from : path);
            String newValue = patchNode.has("value")
                    ? formatNode(patchNode.get("value"))
                    : readNodeValue(newNode, path);
            if (diffType == DiffType.ADD) {
                oldValue = null;
            } else if (diffType == DiffType.REMOVE) {
                newValue = null;
            }
            items.add(new DiffItem(convertJsonPointer(path), oldValue, newValue, diffType));
        }
        return items;
    }

    private List<DiffItem> applyRules(List<DiffItem> items, List<DiffRule> rules) {
        List<DiffItem> result = new ArrayList<>();
        for (DiffItem item : items) {
            boolean shouldReport = true;
            for (DiffRule rule : rules) {
                if (!rule.isEnable() || rule.getRuleType() == DiffRuleType.SORT) {
                    continue;
                }
                if (!matchesRule(item.getFieldPath(), rule.getFieldPath())) {
                    continue;
                }
                DiffRuleExecutor executor = ruleExecutorRegistry.getExecutor(rule.getRuleType());
                if (executor != null && !executor.shouldReport(item, rule)) {
                    shouldReport = false;
                    break;
                }
            }
            if (shouldReport) {
                result.add(item);
            }
        }
        return result;
    }

    private boolean matchesRule(String fieldPath, String rawRulePath) {
        String rulePath = normalizeRulePath(rawRulePath);
        if ("*".equals(rulePath)) {
            return true;
        }
        if (rulePath.equals(fieldPath)) {
            return true;
        }
        if (rulePath.endsWith(".*")) {
            String prefix = rulePath.substring(0, rulePath.length() - 2);
            if (prefix.contains("[*]")) {
                String prefixRegex = toArrayWildcardRegex(prefix);
                return fieldPath.matches(prefixRegex + "(?:$|\\..+|\\[\\d+\\].+)");
            }
            if (fieldPath.equals(prefix)) {
                return true;
            }
            return fieldPath.startsWith(prefix + ".") || fieldPath.startsWith(prefix + "[");
        }
        if (rulePath.contains("[*]")) {
            String regex = toArrayWildcardRegex(rulePath);
            return fieldPath.matches(regex);
        }
        return false;
    }

    private String toArrayWildcardRegex(String normalizedRulePath) {
        return normalizedRulePath
                .replace(".", "\\.")
                .replace("[*]", "\\[\\d+\\]");
    }

    private String normalizeRulePath(String rulePath) {
        if (rulePath == null || rulePath.isBlank()) {
            return "";
        }
        String path = rulePath.trim();
        if (path.startsWith("$.")) {
            path = path.substring(2);
        } else if (path.startsWith("$")) {
            path = path.substring(1);
        }
        return path;
    }

    private DiffType convertOp(String op) {
        return switch (op) {
            case "add" -> DiffType.ADD;
            case "remove" -> DiffType.REMOVE;
            default -> DiffType.MODIFY;
        };
    }

    private String convertJsonPointer(String pointer) {
        if (pointer == null || pointer.isBlank() || "/".equals(pointer)) {
            return "$";
        }
        String[] tokens = pointer.substring(1).split("/");
        StringBuilder builder = new StringBuilder();
        for (String token : tokens) {
            String part = token.replace("~1", "/").replace("~0", "~");
            if (isNumeric(part)) {
                builder.append('[').append(part).append(']');
            } else {
                if (!builder.isEmpty()) {
                    builder.append('.');
                }
                builder.append(part);
            }
        }
        return builder.toString();
    }

    private boolean isNumeric(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private String readNodeValue(JsonNode root, String pointer) {
        if (pointer == null || pointer.isBlank()) {
            return formatNode(root);
        }
        JsonNode node = root.at(pointer);
        return node.isMissingNode() ? null : formatNode(node);
    }

    private String formatNode(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return node.textValue();
        }
        return node.toString();
    }
}
