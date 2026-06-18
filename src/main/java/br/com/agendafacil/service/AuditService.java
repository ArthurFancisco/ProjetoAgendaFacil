package br.com.agendafacil.service;

import br.com.agendafacil.entity.AppUser;
import br.com.agendafacil.entity.AuditLog;
import br.com.agendafacil.entity.Establishment;
import br.com.agendafacil.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final AuditLogRepository auditLogRepository;
    private final RequestInfoService requestInfoService;
    private final HashService hashService;

    public AuditService(AuditLogRepository auditLogRepository, RequestInfoService requestInfoService, HashService hashService) {
        this.auditLogRepository = auditLogRepository;
        this.requestInfoService = requestInfoService;
        this.hashService = hashService;
    }

    public void record(AppUser user, Establishment establishment, String action, String entity, String entityId, String details, HttpServletRequest request) {
        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setEstablishment(establishment);
        log.setAction(action);
        log.setEntity(entity);
        log.setEntityId(entityId);
        log.setDetails(details);
        log.setIpHash(hashService.hash(requestInfoService.clientIp(request)));
        auditLogRepository.save(log);
    }
}
