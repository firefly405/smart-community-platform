package com.smartcommunity.controller;

import com.smartcommunity.entity.SysUser;
import com.smartcommunity.security.AuthInterceptor;
import com.smartcommunity.security.JwtUtils;
import com.smartcommunity.security.UserContext;
import com.smartcommunity.service.SysUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TenantAccessTest {
    private final JwtUtils jwt = mock(JwtUtils.class);
    private final SysUserService users = mock(SysUserService.class);
    private final AuthInterceptor interceptor = new AuthInterceptor(jwt, users);
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    private void identity(int type) {
        request.addHeader("Authorization", "Bearer test-token");
        when(jwt.getUserId("test-token")).thenReturn(23L);
        SysUser user = new SysUser();
        user.setId(23L);
        user.setUsername("tenant001");
        user.setUserType(type);
        user.setStatus(1);
        when(users.getById(23L)).thenReturn(user);
    }

    private HandlerMethod tenantPortal() throws Exception {
        return new HandlerMethod(mock(TenantPortalController.class),
                TenantPortalController.class.getMethod("summary"));
    }

    private HandlerMethod resident() throws Exception {
        return new HandlerMethod(mock(ResidentController.class), ResidentController.class.getMethod("summary"));
    }

    @AfterEach
    void clear() {
        UserContext.clear();
    }

    @Test
    void tenantCanUseTenantPortal() throws Exception {
        identity(3);
        assertTrue(interceptor.preHandle(request, response, tenantPortal()));
        assertEquals(23L, UserContext.getUserId());
    }

    @Test
    void tenantCannotUseResidentPortal() throws Exception {
        identity(3);
        assertFalse(interceptor.preHandle(request, response, resident()));
        assertEquals(403, response.getStatus());
    }

    @Test
    void ownerCannotUseTenantPortal() throws Exception {
        identity(1);
        assertFalse(interceptor.preHandle(request, response, tenantPortal()));
        assertEquals(403, response.getStatus());
    }

    @Test
    void administratorCannotImpersonateTenantPortal() throws Exception {
        identity(5);
        assertFalse(interceptor.preHandle(request, response, tenantPortal()));
        assertEquals(403, response.getStatus());
    }
}
