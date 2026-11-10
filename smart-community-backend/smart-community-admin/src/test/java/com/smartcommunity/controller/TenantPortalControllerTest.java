package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.common.exception.BusinessException;
import com.smartcommunity.entity.*;
import com.smartcommunity.security.UserContext;
import com.smartcommunity.service.*;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TenantPortalControllerTest {
    private final SysUserService users = mock(SysUserService.class);
    private final BaseTenantService tenants = mock(BaseTenantService.class);
    private final BaseHouseService houses = mock(BaseHouseService.class);
    private final FeeBillService bills = mock(FeeBillService.class);
    private final PropRepairService repairs = mock(PropRepairService.class);
    private final PropComplaintService complaints = mock(PropComplaintService.class);
    private final TenantPortalController controller = new TenantPortalController(users, tenants, houses, bills,
            repairs, complaints, mock(PropNoticeService.class), mock(LifeActivityService.class),
            mock(SocialPhoneService.class), mock(BizDisplayService.class));

    @BeforeAll
    static void tables() {
        for (Class<?> type : List.of(BaseTenant.class, BaseHouse.class, FeeBill.class, PropRepair.class,
                PropComplaint.class, PropNotice.class, LifeActivity.class, SocialPhone.class, SysUser.class)) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), type);
        }
    }

    @BeforeEach
    void setup() {
        UserContext.setUserId(23L);
        SysUser user = new SysUser();
        user.setId(23L);
        user.setPhone("13800138000");
        user.setStatus(1);
        user.setUserType(3);
        when(users.getById(23L)).thenReturn(user);

        BaseTenant tenant = new BaseTenant();
        tenant.setId(7L);
        tenant.setOwnerId(1L);
        tenant.setHouseId(6L);
        tenant.setPhone("13800138000");
        tenant.setRealName("Tenant");
        tenant.setStatus(1);
        tenant.setLeaseStartDate(LocalDate.now().minusDays(30));
        tenant.setLeaseEndDate(LocalDate.now().plusDays(30));
        when(tenants.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(tenant));

        BaseHouse house = new BaseHouse();
        house.setId(6L);
        house.setHouseNo("2-201");
        when(houses.getById(6L)).thenReturn(house);
    }

    @AfterEach
    void cleanup() {
        UserContext.clear();
    }

    private TenantPortalController.ServiceRequest input(Long houseId) {
        return new TenantPortalController.ServiceRequest(houseId, "Water leak", "Kitchen pipe needs repair",
                1, 1, "Tenant", "13800138000");
    }

    @Test
    void activeTenantLookupRequiresPhoneStatusAndLeaseDates() {
        controller.summary();
        ArgumentCaptor<LambdaQueryWrapper<BaseTenant>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(tenants).list(captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertTrue(sql.contains("phone ="));
        assertTrue(sql.contains("status ="));
        assertTrue(sql.contains("lease_start_date <="));
        assertTrue(sql.contains("lease_end_date >="));
        assertTrue(captor.getValue().getParamNameValuePairs().containsValue("13800138000"));
    }

    @Test
    void missingOrExpiredLeaseIsReportedWithoutExposingScopedData() {
        when(tenants.list(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        assertEquals(false, controller.summary().getData().get("bound"));
        verifyNoInteractions(houses, bills, repairs, complaints);
        assertEquals(403, assertThrows(BusinessException.class, controller::house).getCode());
    }

    @Test
    void ambiguousActiveLeaseIsRejected() {
        BaseTenant other = new BaseTenant();
        other.setHouseId(8L);
        when(tenants.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(new BaseTenant(), other));
        assertEquals(409, assertThrows(BusinessException.class, controller::summary).getCode());
        verifyNoInteractions(houses, bills, repairs, complaints);
    }

    @Test
    void billsAreScopedToRentedHouseInsteadOfLandlord() {
        when(bills.page(any(Page.class), any())).thenReturn(new Page<FeeBill>());
        controller.list("bills", 1, 10);
        ArgumentCaptor<LambdaQueryWrapper<FeeBill>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(bills).page(any(Page.class), captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertTrue(sql.contains("house_id ="));
        assertTrue(sql.contains("bill_end_date >="));
        assertTrue(sql.contains("bill_start_date <="));
        assertFalse(sql.contains("owner_id ="));
        assertTrue(captor.getValue().getParamNameValuePairs().containsValue(6L));
    }

    @Test
    void repairHistoryIsScopedToHouseAndCurrentAccount() {
        when(repairs.page(any(Page.class), any())).thenReturn(new Page<PropRepair>());
        controller.list("repairs", 1, 10);
        ArgumentCaptor<LambdaQueryWrapper<PropRepair>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(repairs).page(any(Page.class), captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertTrue(sql.contains("house_id ="));
        assertTrue(sql.contains("create_by ="));
        assertTrue(captor.getValue().getParamNameValuePairs().containsValue(6L));
        assertTrue(captor.getValue().getParamNameValuePairs().containsValue(23L));
    }

    @Test
    void submittedRepairUsesLeaseScopeAndRecordsAccountOwnership() {
        controller.repair(input(6L));
        ArgumentCaptor<PropRepair> captor = ArgumentCaptor.forClass(PropRepair.class);
        verify(repairs).save(captor.capture());
        PropRepair repair = captor.getValue();
        assertEquals(1L, repair.getOwnerId());
        assertEquals(6L, repair.getHouseId());
        assertEquals(23L, repair.getCreateBy());
        assertEquals(0, repair.getStatus());
        assertNull(repair.getHandlerId());
    }

    @Test
    void foreignHouseCannotBeUsedForSubmissions() {
        assertEquals(403, assertThrows(BusinessException.class, () -> controller.repair(input(99L))).getCode());
        assertEquals(403, assertThrows(BusinessException.class, () -> controller.complaint(input(99L))).getCode());
        verifyNoInteractions(repairs, complaints);
    }

    @Test
    void anotherAccountsRepairCannotBeEvaluated() {
        PropRepair repair = new PropRepair();
        repair.setHouseId(6L);
        repair.setCreateBy(99L);
        repair.setStatus(3);
        when(repairs.getById(8L)).thenReturn(repair);
        assertEquals(404, assertThrows(BusinessException.class,
                () -> controller.evaluate(8L, new TenantPortalController.Evaluation(5, "good"))).getCode());
        verify(repairs, never()).evaluate(any(), any(), any());
    }

    @Test
    void nonTenantCannotCallPortalControllerDirectly() {
        SysUser admin = new SysUser();
        admin.setStatus(1);
        admin.setUserType(5);
        when(users.getById(23L)).thenReturn(admin);
        assertEquals(403, assertThrows(BusinessException.class, controller::summary).getCode());
        verifyNoInteractions(tenants);
    }

    @Test
    void tenantCannotBreakLeaseBindingByChangingPhone() {
        SysUser request = new SysUser();
        request.setPhone("13900139000");
        assertEquals(400, assertThrows(BusinessException.class,
                () -> new UserController(users).updateProfile(request)).getCode());
        verify(users, never()).updateById(any());
    }

    @Test
    void tenantProfileUpdateWithoutPhonePreservesLeaseBinding() {
        SysUser request = new SysUser();
        request.setRealName("New Name");
        SysUser updated = new UserController(users).updateProfile(request).getData();
        assertEquals("13800138000", updated.getPhone());
        verify(users).updateById(updated);
    }
}
