package swp490.greeenslot.config;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import swp490.greeenslot.entity.AuditLog;
import swp490.greeenslot.repository.AuditLogRepository;

import java.time.LocalDateTime;
import java.util.Arrays;

@Aspect
@Component
public class AuditLoggingAspect {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Around("execution(* swp490.greeenslot.controller..*(..)) && " +
            "(@annotation(org.springframework.security.access.prepost.PreAuthorize) || " +
            "@within(org.springframework.security.access.prepost.PreAuthorize))")
    public Object logAudit(ProceedingJoinPoint joinPoint) throws Throwable {
        LocalDateTime performedAt = LocalDateTime.now();
        
        // Retrieve request attributes
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String ipAddress = "UNKNOWN";
        String requestUri = "UNKNOWN";
        String method = "UNKNOWN";
        
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            requestUri = request.getRequestURI();
            method = request.getMethod();
            
            // Resolve IP address (handle proxies)
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                ipAddress = xForwardedFor.split(",")[0].trim();
            } else {
                ipAddress = request.getRemoteAddr();
            }
        }

        // Retrieve current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String performedBy = "anonymousUser";
        if (authentication != null && authentication.isAuthenticated()) {
            performedBy = authentication.getName();
        }

        // Infer Entity ID and Entity Type from method parameters
        Long entityId = null;
        String entityType = null;
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        if (parameterNames != null && args != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                String paramName = parameterNames[i];
                if (paramName.toLowerCase().contains("id") && args[i] instanceof Long) {
                    entityId = (Long) args[i];
                    entityType = paramName.replace("Id", "");
                    if (entityType.isEmpty()) {
                        String controllerName = joinPoint.getTarget().getClass().getSimpleName();
                        entityType = controllerName.replace("Controller", "");
                    }
                    if (entityType.length() > 0) {
                        entityType = entityType.substring(0, 1).toUpperCase() + entityType.substring(1);
                    }
                    break;
                }
            }
        }

        String action = method + " " + requestUri;
        String methodName = signature.getDeclaringTypeName() + "." + signature.getName();
        
        Object result;
        String status = "SUCCESS";
        String detailsStr = "";
        
        try {
            result = joinPoint.proceed();
            detailsStr = "Executed " + methodName + " successfully with args: " + Arrays.toString(args);
        } catch (Throwable ex) {
            status = "FAILED";
            detailsStr = "Failed executing " + methodName + " with args: " + Arrays.toString(args) + ". Error: " + ex.getMessage();
            throw ex;
        } finally {
            AuditLog auditLog = new AuditLog();
            auditLog.setAction(action + " (" + status + ")");
            auditLog.setEntityType(entityType);
            auditLog.setEntityId(entityId);
            auditLog.setPerformedBy(performedBy);
            auditLog.setPerformedAt(performedAt);
            auditLog.setDetails(detailsStr);
            auditLog.setIpAddress(ipAddress);
            
            try {
                auditLogRepository.save(auditLog);
            } catch (Exception dbEx) {
                System.err.println("AuditLoggingAspect: Failed to persist audit log: " + dbEx.getMessage());
            }
        }
        
        return result;
    }
}
