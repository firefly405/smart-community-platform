package com.smartcommunity.controller;

import com.smartcommunity.common.exception.BusinessException;
import com.smartcommunity.entity.GuardVisitor;
import com.smartcommunity.entity.PropEquipment;
import com.smartcommunity.entity.PropEquipmentMaintain;
import com.smartcommunity.entity.BaseOwner;
import com.smartcommunity.entity.BaseVehicle;
import com.smartcommunity.entity.SysUser;
import com.smartcommunity.security.UserContext;
import com.smartcommunity.service.*;
import com.smartcommunity.service.impl.BaseVehicleServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StructureFlowTest {
    @Test
    void duplicateVehiclePlateIsRejectedBeforeParkingChanges() {
        BaseParkingService parking = mock(BaseParkingService.class);
        BaseOwnerService owners = mock(BaseOwnerService.class);
        when(owners.getById(1L)).thenReturn(new BaseOwner());
        BaseVehicleServiceImpl vehicles = spy(new BaseVehicleServiceImpl(parking, owners));
        doReturn(1L).when(vehicles).count(any());
        BaseVehicle vehicle = new BaseVehicle();
        vehicle.setOwnerId(1L);
        vehicle.setParkingId(2L);
        vehicle.setPlateNo(" a12345 ");

        BusinessException error = assertThrows(BusinessException.class, () -> vehicles.createWithParking(vehicle));

        assertEquals(400, error.getCode());
        assertEquals("车牌号已存在，请勿重复添加", error.getMessage());
        verifyNoInteractions(parking);
    }

    @Test
    void profileUpdateOnlyChangesCurrentUsersEditableFields() {
        SysUserService users = mock(SysUserService.class);
        SysUser current = new SysUser();
        current.setId(7L);
        current.setUsername("admin");
        current.setStatus(1);
        when(users.getById(7L)).thenReturn(current);
        SysUser request = new SysUser();
        request.setId(99L);
        request.setUsername("changed-account");
        request.setStatus(0);
        request.setRealName(" Updated Name ");
        request.setPhone("13800138000");
        request.setEmail("user@example.com");
        request.setGender(2);

        UserContext.setUserId(7L);
        try {
            new UserController(users).updateProfile(request);
        } finally {
            UserContext.clear();
        }

        assertEquals(7L, current.getId());
        assertEquals("admin", current.getUsername());
        assertEquals(1, current.getStatus());
        assertEquals("Updated Name", current.getRealName());
        verify(users).updateById(current);
    }

    @Test
    void avatarUploadRejectsUnsupportedFilesBeforeSaving() {
        SysUserService users = mock(SysUserService.class);
        MockMultipartFile file = new MockMultipartFile("file", "avatar.gif", "image/gif", new byte[] { 1, 2, 3 });
        assertThrows(BusinessException.class, () -> new UserController(users).uploadAvatar(file));
        verifyNoInteractions(users);
    }

    @Test
    void avatarUploadStoresImageAndUpdatesCurrentUser(@TempDir Path uploadDir) {
        SysUserService users = mock(SysUserService.class);
        UserController controller = new UserController(users);
        ReflectionTestUtils.setField(controller, "uploadDir", uploadDir.toString());
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[] { 1, 2, 3 });

        UserContext.setUserId(7L);
        String url;
        try {
            url = controller.uploadAvatar(file).getData().get("url");
        } finally {
            UserContext.clear();
        }

        assertTrue(url.startsWith("/uploads/avatars/"));
        assertTrue(Files.exists(uploadDir.resolve(url.substring("/uploads/".length()))));
        verify(users).updateById(argThat(user -> user.getId() == 7L && url.equals(user.getAvatar())));
    }

    @Test
    void visitorDetailUsesRequestedIdAndDoesNotFallBackToList() {
        GuardVisitorService visitors = mock(GuardVisitorService.class);
        GuardVisitor selected = new GuardVisitor();
        selected.setId(42L);
        selected.setVisitorName("selected");
        when(visitors.getById(42L)).thenReturn(selected);
        VisitorController controller = new VisitorController(visitors,
            mock(GuardAccessLogService.class), mock(GuardAccessService.class), mock(BizDisplayService.class));
        assertSame(selected, controller.detail(42L).getData());
        assertFalse(controller.detail(999L).isSuccess());
        verify(visitors).getById(42L);
        verify(visitors).getById(999L);
        verifyNoMoreInteractions(visitors);
    }

    @Test
    void maintenanceRejectsUnknownEquipmentBeforeSaving() {
        PropEquipmentMaintainService records = mock(PropEquipmentMaintainService.class);
        MaintainController controller = new MaintainController(records, mock(PropEquipmentService.class));
        PropEquipmentMaintain record = validRecord();
        assertThrows(BusinessException.class, () -> controller.create(record));
        verifyNoInteractions(records);
    }

    @Test
    void maintenanceRejectsNegativeCostAndMissingDate() {
        PropEquipmentMaintainService records = mock(PropEquipmentMaintainService.class);
        PropEquipmentService equipment = mock(PropEquipmentService.class);
        when(equipment.getById(8L)).thenReturn(new PropEquipment());
        MaintainController controller = new MaintainController(records, equipment);
        PropEquipmentMaintain record = validRecord();
        record.setCost(BigDecimal.valueOf(-1));
        assertThrows(BusinessException.class, () -> controller.create(record));
        record.setCost(BigDecimal.ZERO);
        record.setMaintainDate(null);
        assertThrows(BusinessException.class, () -> controller.create(record));
        verifyNoInteractions(records);
    }

    @Test
    void creatingMaintenanceCannotOverwriteExistingRecordOrSkipPendingState() {
        PropEquipmentMaintainService records = mock(PropEquipmentMaintainService.class);
        PropEquipmentService equipment = mock(PropEquipmentService.class);
        when(equipment.getById(8L)).thenReturn(new PropEquipment());
        MaintainController controller = new MaintainController(records, equipment);
        PropEquipmentMaintain record = validRecord();
        record.setId(99L);
        record.setStatus(2);
        controller.create(record);
        assertNull(record.getId());
        assertEquals(0, record.getStatus());
        verify(records).save(record);
    }

    @Test
    void completionOfMissingMaintenanceDoesNotReportSuccess() {
        MaintainController controller = new MaintainController(
            mock(PropEquipmentMaintainService.class), mock(PropEquipmentService.class));
        assertThrows(BusinessException.class, () -> controller.complete(404L));
    }

    @Test
    void aiConfigurationIsRestrictedToAdministrators() {
        SysUserService users = mock(SysUserService.class);
        SysUser staff = new SysUser();
        staff.setUserType(4);
        when(users.getById(7L)).thenReturn(staff);
        AiConfigController controller = new AiConfigController(new MockEnvironment(), users);

        UserContext.setUserId(7L);
        try {
            BusinessException error = assertThrows(BusinessException.class, controller::getConfig);
            assertEquals(403, error.getCode());
        } finally {
            UserContext.clear();
        }
    }

    @Test
    void backendRestartIsRestrictedToAdministrators() {
        SysUserService users = mock(SysUserService.class);
        SysUser staff = new SysUser();
        staff.setUserType(4);
        when(users.getById(7L)).thenReturn(staff);
        AiConfigController controller = new AiConfigController(new MockEnvironment(), users);

        UserContext.setUserId(7L);
        try {
            BusinessException error = assertThrows(BusinessException.class, controller::restartBackend);
            assertEquals(403, error.getCode());
        } finally {
            UserContext.clear();
        }
    }

    @Test
    void enablingAiWithoutAnApiKeyIsRejected(@TempDir Path tempDir) {
        SysUserService users = mock(SysUserService.class);
        SysUser admin = new SysUser();
        admin.setUserType(5);
        when(users.getById(1L)).thenReturn(admin);
        AiConfigController controller = new AiConfigController(new MockEnvironment(), users);
        ReflectionTestUtils.setField(controller, "configPath", tempDir.resolve("ai.properties").toString());

        UserContext.setUserId(1L);
        try {
            AiConfigController.AiConfigRequest request = new AiConfigController.AiConfigRequest(
                    true, "https://api.example.com/v1", "", "", "");
            BusinessException error = assertThrows(BusinessException.class, () -> controller.saveConfig(request));
            assertEquals(400, error.getCode());
        } finally {
            UserContext.clear();
        }
    }

    @Test
    void aiConfigurationRejectsPrivateApiEndpoints(@TempDir Path tempDir) {
        SysUserService users = mock(SysUserService.class);
        SysUser admin = new SysUser();
        admin.setUserType(5);
        when(users.getById(1L)).thenReturn(admin);
        AiConfigController controller = new AiConfigController(new MockEnvironment(), users);
        ReflectionTestUtils.setField(controller, "configPath", tempDir.resolve("ai.properties").toString());

        UserContext.setUserId(1L);
        try {
            AiConfigController.AiConfigRequest request = new AiConfigController.AiConfigRequest(
                    true, "https://127.0.0.1/v1", "test-key", "", "");
            BusinessException error = assertThrows(BusinessException.class, () -> controller.saveConfig(request));
            assertEquals(400, error.getCode());
            assertTrue(error.getMessage().contains("本机或内网"));
        } finally {
            UserContext.clear();
        }
    }

    @Test
    void aiConfigurationAutomaticallySelectsChatAndEmbeddingModels() {
        String chat = ReflectionTestUtils.invokeMethod(AiConfigController.class, "chooseChatModel",
                java.util.List.of("BAAI/bge-m3", "image-model", "Qwen/Qwen2.5-7B-Instruct"), "");
        String embedding = ReflectionTestUtils.invokeMethod(AiConfigController.class, "chooseEmbeddingModel",
                java.util.List.of("BAAI/bge-m3", "Qwen/Qwen2.5-7B-Instruct"), "");

        assertEquals("Qwen/Qwen2.5-7B-Instruct", chat);
        assertEquals("BAAI/bge-m3", embedding);
    }

    private PropEquipmentMaintain validRecord() {
        PropEquipmentMaintain record = new PropEquipmentMaintain();
        record.setEquipmentId(8L);
        record.setMaintainContent("check");
        record.setMaintainDate(LocalDate.of(2026, 10, 13));
        return record;
    }
}

