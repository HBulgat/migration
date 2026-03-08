package top.bulgat.migration.diff.domain.repository;

import top.bulgat.migration.diff.domain.model.AlertTemplate;

/**
 * 告警模板仓储接口。
 */
public interface AlertTemplateRepository {

    /**
     * 根据 templateKey 查找告警模板。
     *
     * @param templateKey 模板标识
     * @return 对应的模板，未找到时返回 null
     */
    AlertTemplate findByKey(String templateKey);
}
