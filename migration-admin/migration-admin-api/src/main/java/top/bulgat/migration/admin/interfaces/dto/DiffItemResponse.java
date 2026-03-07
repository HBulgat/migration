package top.bulgat.migration.admin.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 接口响应 DTO。
 */
public record DiffItemResponse(
        @JsonProperty("field_path") String fieldPath,
        @JsonProperty("old_value") String oldValue,
        @JsonProperty("new_value") String newValue,
        @JsonProperty("diff_type") String diffType) {
}

