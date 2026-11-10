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

class ResidentAccessTest {
    private final JwtUtils jwt = mock(JwtUtils.class);
    private final SysUserService users = mock(SysUserService.class);
    private final AuthInterceptor interceptor = new AuthInterceptor(jwt, users);
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    private void identity(int type, int status) {
        request.addHeader("Authorization", "Bearer test-token");
        when(jwt.getUserId("test-token")).thenReturn(13L);
        SysUser user = new SysUser(); user.setId(13L); user.setUsername("owner001"); user.setUserType(type); user.setStatus(status);
        when(users.getById(13L)).thenReturn(user);
    }
    private HandlerMethod self() throws Exception {
        return new HandlerMethod(new UserController(users), UserController.class.getMethod("info"));
    }
    private HandlerMethod resident() throws Exception {
        return new HandlerMethod(mock(ResidentController.class), ResidentController.class.getMethod("summary"));
    }
    @AfterEach void clear() { UserContext.clear(); }
    @Test void ownerCanUseOwnProfile() throws Exception {
        identity(1, 1); assertTrue(interceptor.preHandle(request, response, self())); assertEquals(13L, UserContext.getUserId());
    }
    @Test void ownerCanUseResidentServices() throws Exception {
        identity(1, 1); assertTrue(interceptor.preHandle(request, response, resident()));
    }
    @Test void ownerCannotReadUserListsEvenWithResidentLookingUri() throws Exception {
        identity(1, 1); request.setRequestURI("/api/v1/resident/../user/99");
        HandlerMethod management = new HandlerMethod(new UserController(users), UserController.class.getMethod("getById", Long.class));
        assertFalse(interceptor.preHandle(request, response, management)); assertEquals(403, response.getStatus()); assertNull(UserContext.getUserId());
    }
    @Test void ownerCannotAssignRoles() throws Exception {
        identity(1, 1);
        HandlerMethod roles = new HandlerMethod(new UserController(users), UserController.class.getMethod("assignRoles", Long.class, java.util.Map.class));
        assertFalse(interceptor.preHandle(request, response, roles)); assertEquals(403, response.getStatus());
    }
    @Test void ownerCannotCallAnyOtherManagementHandler() {
        identity(1, 1); assertFalse(interceptor.preHandle(request, response, new Object())); assertEquals(403, response.getStatus());
    }
    @Test void disabledAccountLosesAccessImmediately() throws Exception {
        identity(1, 0); assertFalse(interceptor.preHandle(request, response, self())); assertEquals(401, response.getStatus());
    }
    @Test void deletedAccountCannotUseExistingToken() throws Exception {
        identity(1, 1); when(users.getById(13L)).thenReturn(null);
        assertFalse(interceptor.preHandle(request, response, self())); assertEquals(401, response.getStatus());
    }
    @Test void adminCannotImpersonateResident() throws Exception {
        identity(5, 1); assertFalse(interceptor.preHandle(request, response, resident())); assertEquals(403, response.getStatus());
    }
    @Test void tenantDoesNotFallThroughToManagement() throws Exception {
        identity(3, 1);
        HandlerMethod management = new HandlerMethod(new UserController(users), UserController.class.getMethod("getById", Long.class));
        assertFalse(interceptor.preHandle(request, response, management)); assertEquals(403, response.getStatus());
    }
    @Test void adminRetainsExistingAccess() {
        identity(5, 1); assertTrue(interceptor.preHandle(request, response, new Object()));
    }
    @Test void missingTokenIsRejectedAndThreadIdentityCleared() {
        UserContext.setUserId(1L);
        assertFalse(interceptor.preHandle(request, response, new Object())); assertEquals(401, response.getStatus()); assertNull(UserContext.getUserId());
    }
}
