package top.bulgat.migration.admin.domain.repository;

import top.bulgat.migration.admin.domain.model.AlertTemplate;

/**
 * AlertTemplateRepository 定义持久化访问能力。
 */
public interface AlertTemplateRepository {

    /**
     * 根据 templateKey 查找告警模板。
     *
     * @param templateKey 模板标识
     * @return 对应的模板，未找到时返回 null
     */
    AlertTemplate findByTemplateKey(String templateKey);
    
    /**
     * 保存（新增或更新）告警模板。
     *
     * @param template 告警模板
     */
    void save(AlertTemplate template);

    /**
     * 获取所有告警模板。
     *
     * @return 模板列表
     */
    java.util.List<AlertTemplate> findAll();
}
