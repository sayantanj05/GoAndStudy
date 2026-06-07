package com.goandstudybackend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Simple IP allowlist enforcement.
 *
 * Configure via env/property:
 * - app.ip.allowlist (comma-separated CIDRs or plain IPs)
 * - app.ip.allowlist.enabled (true/false)
 *
 * Currently supports entries like: "152.59.163.21/32" or "152.59.163.21".
 */
@Component
public class IpAllowlistFilter extends OncePerRequestFilter {

    private final Set<String> allowlist;
    private final boolean enabled;

    public IpAllowlistFilter(
            @Value("${app.ip.allowlist:}") String allowlistRaw,
            @Value("${app.ip.allowlist.enabled:true}") boolean enabled
    ) {
        this.enabled = enabled;

        if (allowlistRaw == null || allowlistRaw.trim().isEmpty()) {
            this.allowlist = Set.of();
        } else {
            Set<String> tmp = new HashSet<>();
            Arrays.stream(allowlistRaw.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(tmp::add);
            this.allowlist = tmp;
        }
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if (!enabled || allowlist.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = extractClientIp(request);
        if (ip == null || ip.isBlank() || !isAllowed(ip)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }

        String forwarded = request.getHeader("Forwarded");
        if (forwarded != null && forwarded.toLowerCase().contains("for=")) {
            for (String part : forwarded.split(";")) {
                String p = part.trim();
                if (p.toLowerCase().startsWith("for=")) {
                    String val = p.substring(4).trim();
                    return val.replace("\"", "").replace("[", "").replace("]", "");
                }
            }
        }

        return request.getRemoteAddr();
    }

    private boolean isAllowed(String ip) {
        for (String entry : allowlist) {
            String e = entry.trim();
            if (e.endsWith("/32")) {
                String base = e.substring(0, e.length() - 3);
                if (base.equals(ip)) return true;
            } else {
                if (e.equals(ip)) return true;
            }
        }
        return false;
    }
}

