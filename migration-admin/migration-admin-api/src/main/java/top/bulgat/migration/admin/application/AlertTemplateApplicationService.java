package top.bulgat.migration.admin.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.bulgat.common.base.model.PageResult;
import top.bulgat.migration.admin.domain.model.AlertTemplate;
import top.bulgat.migration.admin.domain.repository.AlertTemplateRepository;
import top.bulgat.migration.admin.interfaces.dto.AlertTemplateDTO;
import top.bulgat.migration.admin.interfaces.dto.CreateAlertTemplateRequest;
import top.bulgat.migration.admin.interfaces.dto.UpdateAlertTemplateRequest;
import top.bulgat.common.base.exception.BizException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AlertTemplateApplicationService {

    private final AlertTemplateRepository alertTemplateRepository;

    public AlertTemplateApplicationService(AlertTemplateRepository alertTemplateRepository) {
        this.alertTemplateRepository = alertTemplateRepository;
    }

    public void createAlertTemplate(CreateAlertTemplateRequest request) {
        AlertTemplate existing = alertTemplateRepository.findByTemplateKey(request.getTemplateKey());
        if (existing != null) {
            throw new BizException("templateKey already exists");
        }

        AlertTemplate template = new AlertTemplate(
                request.getChannel(),
                request.getName(),
                request.getTemplate()
        );
        template.initTemplateKey(request.getTemplateKey());
        template.initCreateTime();

        alertTemplateRepository.save(template);
        log.info("Alert template created: {}", request.getTemplateKey());
    }

    public void updateAlertTemplate(UpdateAlertTemplateRequest request) {
        AlertTemplate existing = alertTemplateRepository.findByTemplateKey(request.getTemplateKey());
        if (existing == null) {
            throw new BizException("templateKey does not exist");
        }

        existing.update(request.getChannel(), request.getName(), request.getTemplate());
        
        alertTemplateRepository.save(existing);
        log.info("Alert template updated: {}", request.getTemplateKey());
    }

    public PageResult<AlertTemplateDTO> listAlertTemplates(String channel) {
        List<AlertTemplate> templates = alertTemplateRepository.findAll();
        // Since it's from config center, there's no real pagination. We wrap all in a single page.
        List<AlertTemplateDTO> dtos = templates.stream()
                .filter(t -> channel == null || channel.isEmpty() || t.getChannel().name().equalsIgnoreCase(channel))
                .map(t -> {
                    AlertTemplateDTO dto = AlertTemplateDTO.from(t);
                    return dto;
                })
                .collect(Collectors.toList());
        
        return new PageResult<>(1, dtos.size(), dtos.size(), dtos);
    }
}
