package com.example.theatre.security;

import com.example.theatre.service.AdminActivityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminActivityInterceptor implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(AdminActivityInterceptor.class);
    private final AdminActivityService activities;

    public AdminActivityInterceptor(AdminActivityService activities) {
        this.activities = activities;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception exception) {
        if (exception != null || response.getStatus() < 200 || response.getStatus() >= 300) return;
        String path = request.getRequestURI();
        String method = request.getMethod();
        String[] parts = path.split("/");
        if (parts.length < 3 || !"api".equals(parts[1])) return;
        String resource = parts[2];
        String suffix = parts[parts.length - 1];
        String action;
        boolean paymentAction = "payments".equals(resource)
                && ("confirm-cash".equals(suffix) || "refund-cash".equals(suffix));
        boolean catalogueMutation = "POST".equals(method) || "PUT".equals(method)
                || "PATCH".equals(method) || "DELETE".equals(method);
        boolean trackedResource = switch (resource) {
            case "movies", "theatres", "screens", "shows", "seats", "users" -> true;
            default -> false;
        };
        if (!paymentAction && !(catalogueMutation && trackedResource)) return;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getAuthorities().stream().noneMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))
                || !(authentication.getPrincipal() instanceof Jwt jwt)) return;
        Long adminId;
        try { adminId = Long.valueOf(jwt.getSubject()); }
        catch (NumberFormatException ignored) { return; }

        Long resourceId = parseResourceId(parts, response);
        action = switch (suffix) {
            case "confirm-cash" -> "CONFIRM_CASH";
            case "refund-cash" -> "REFUND_CASH";
            default -> switch (method) {
                case "POST" -> "CREATE";
                case "PUT", "PATCH" -> "UPDATE";
                case "DELETE" -> "DELETE";
                default -> "CHANGE";
            };
        };
        try {
            activities.record(adminId, action, resource, resourceId);
        } catch (RuntimeException failure) {
            log.warn("Could not record admin activity for {} {}", method, path, failure);
        }
    }

    private Long parseResourceId(String[] parts, HttpServletResponse response) {
        String candidate = parts.length > 3 ? parts[3] : null;
        if (candidate != null && candidate.matches("\\d+")) {
            try { return Long.valueOf(candidate); } catch (NumberFormatException ignored) { return null; }
        }
        String location = response.getHeader("Location");
        if (location == null) return null;
        String[] locationParts = location.split("/");
        String last = locationParts[locationParts.length - 1];
        try { return Long.valueOf(last); } catch (NumberFormatException ignored) { return null; }
    }
}
