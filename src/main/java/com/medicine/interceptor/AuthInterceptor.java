package com.medicine.interceptor;

import com.medicine.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final org.slf4j.Logger accessLogger = org.slf4j.LoggerFactory.getLogger("com.medicine.access");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        String remoteAddr = request.getRemoteAddr();

        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        // Log access
        if (user != null) {
            accessLogger.debug("Access - URI: {}, Method: {}, User: {}, IP: {}",
                requestURI, method, user.getUsername(), remoteAddr);
        } else {
            accessLogger.debug("Access - URI: {}, Method: {}, User: Anonymous, IP: {}",
                requestURI, method, remoteAddr);
        }

        // Allow login page and static resources
        if (requestURI.startsWith("/login") ||
            requestURI.startsWith("/css") ||
            requestURI.startsWith("/js") ||
            requestURI.startsWith("/images")) {
            return true;
        }

        // Check authentication
        if (user == null) {
            log.debug("Unauthenticated access attempt to: {}", requestURI);
            response.sendRedirect("/login");
            return false;
        }

        return true;
    }
}
