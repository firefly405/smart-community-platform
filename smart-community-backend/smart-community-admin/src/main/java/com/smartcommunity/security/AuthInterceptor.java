package com.smartcommunity.security;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.method.HandlerMethod;
import com.smartcommunity.controller.AuthController;
import com.smartcommunity.controller.UserController;
import com.smartcommunity.controller.ResidentController;
import com.smartcommunity.controller.ResidentSocialController;
import com.smartcommunity.controller.ResidentQaController;
import com.smartcommunity.controller.TenantPortalController;
import com.smartcommunity.entity.SysUser;
import com.smartcommunity.service.SysUserService;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtils jwtUtils;
    private final SysUserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        UserContext.clear();
        String token = extractToken(request);
        if (StrUtil.isBlank(token)) {
            response.setStatus(401);
            return false;
        }

        Long userId = jwtUtils.getUserId(token);
        if (userId == null) {
            response.setStatus(401);
            return false;
        }

        SysUser user = userService.getById(userId);
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())) {
            response.setStatus(401);
            return false;
        }
        boolean selfService = handler instanceof HandlerMethod method && (
                (method.getBeanType() == UserController.class && Set.of("info", "updateProfile", "changePassword", "uploadAvatar").contains(method.getMethod().getName()))
                || (method.getBeanType() == AuthController.class && method.getMethod().getName().equals("logout")));
        boolean resident = handler instanceof HandlerMethod method && Set.of(ResidentController.class,
                ResidentSocialController.class, ResidentQaController.class).contains(method.getBeanType());
        boolean tenantPortal = handler instanceof HandlerMethod method
                && method.getBeanType() == TenantPortalController.class;
        Integer type = user.getUserType();
        // Authorize the resolved handler, not a URL prefix that can be bypassed by alternate paths.
        boolean management = Integer.valueOf(4).equals(type) || Integer.valueOf(5).equals(type);
        boolean allowed = selfService
                || (resident && Integer.valueOf(1).equals(type))
                || (tenantPortal && Integer.valueOf(3).equals(type))
                || (management && !resident && !tenantPortal);
        if (!allowed) {
            response.setStatus(403);
            return false;
        }
        String username = user.getUsername();
        UserContext.setUserId(userId);
        UserContext.setUsername(username);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StrUtil.isNotBlank(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
