package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.common.exception.BusinessException;
import com.smartcommunity.entity.*;
import com.smartcommunity.mapper.BaseOwnerHouseMapper;
import com.smartcommunity.security.UserContext;
import com.smartcommunity.service.*;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResidentControllerTest {
    private final SysUserService users = mock(SysUserService.class);
    private final BaseOwnerService owners = mock(BaseOwnerService.class);
    private final BaseOwnerHouseMapper relations = mock(BaseOwnerHouseMapper.class);
    private final BaseHouseService houses = mock(BaseHouseService.class);
    private final FeeBillService bills = mock(FeeBillService.class);
    private final PropRepairService repairs = mock(PropRepairService.class);
    private final PropComplaintService complaints = mock(PropComplaintService.class);
    private final ResidentController controller = new ResidentController(users, owners, relations, houses, mock(BaseVehicleService.class), bills, repairs, complaints, mock(PropNoticeService.class), mock(LifeActivityService.class), mock(SocialPhoneService.class), mock(BizDisplayService.class));

    @BeforeAll static void tables() {
        for (Class<?> type : List.of(BaseOwner.class, BaseOwnerHouse.class, BaseHouse.class, FeeBill.class)) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), type);
        }
    }
    @BeforeEach void setup() {
        UserContext.setUserId(13L);
        SysUser user = new SysUser(); user.setId(13L); user.setStatus(1); user.setUserType(1); when(users.getById(13L)).thenReturn(user);
        BaseOwner owner = new BaseOwner(); owner.setId(1L); owner.setUserId(13L); when(owners.getOne(any())).thenReturn(owner);
        BaseOwnerHouse relation = new BaseOwnerHouse(); relation.setHouseId(10L); when(relations.selectList(any())).thenReturn(List.of(relation));
    }
    @AfterEach void cleanup() { UserContext.clear(); }
    private ResidentController.ServiceRequest input(Long houseId) {
        return new ResidentController.ServiceRequest(houseId, "Water leak", "Kitchen pipe needs repair", 1, 1, "Owner", "13800138000");
    }
    @Test void repairUsesBoundOwnerIdNotAccountIdAndStartsPending() {
        controller.repair(input(10L));
        ArgumentCaptor<PropRepair> captor = ArgumentCaptor.forClass(PropRepair.class); verify(repairs).save(captor.capture());
        assertEquals(1L, captor.getValue().getOwnerId()); assertEquals(10L, captor.getValue().getHouseId()); assertEquals(0, captor.getValue().getStatus()); assertNull(captor.getValue().getHandlerId());
    }
    @Test void forgedManagementFieldsAreNotAcceptedByResidentRequest() throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper()
                .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        ResidentController.ServiceRequest input = mapper.readValue("""
                {"id":99,"ownerId":2,"status":3,"handlerId":1,"houseId":10,
                 "title":"Repair","content":"Broken window","type":1,"urgency":1,
                 "contactName":"Owner","contactPhone":"13800138000"}
                """, ResidentController.ServiceRequest.class);
        controller.repair(input);
        ArgumentCaptor<PropRepair> captor = ArgumentCaptor.forClass(PropRepair.class); verify(repairs).save(captor.capture());
        assertNull(captor.getValue().getId()); assertEquals(1L, captor.getValue().getOwnerId());
        assertEquals(0, captor.getValue().getStatus()); assertNull(captor.getValue().getHandlerId());
    }
    @Test void ownerWithoutHousesNeverRunsUnfilteredHouseQuery() {
        when(relations.selectList(any())).thenReturn(List.of());
        Page<?> result = (Page<?>) controller.list("houses", 1, 10).getData();
        assertEquals(0, result.getTotal()); verifyNoInteractions(houses);
    }
    @Test void foreignHouseIsRejectedBeforeAnyWrite() {
        assertEquals(403, assertThrows(BusinessException.class, () -> controller.repair(input(99L))).getCode()); verifyNoInteractions(repairs);
        assertEquals(403, assertThrows(BusinessException.class, () -> controller.complaint(input(99L))).getCode()); verifyNoInteractions(complaints);
    }
    @Test void unboundOwnerCannotFallBackToAccountId() {
        when(owners.getOne(any())).thenReturn(null);
        assertEquals(false, controller.summary().getData().get("bound"));
        assertThrows(BusinessException.class, () -> controller.list("bills", 1, 10)); verifyNoInteractions(bills);
    }
    @Test void billsAreFilteredByBoundOwner() {
        when(bills.page(any(Page.class), any())).thenReturn(new Page<FeeBill>());
        controller.list("bills", 1, 10);
        ArgumentCaptor<LambdaQueryWrapper<FeeBill>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(bills).page(any(Page.class), captor.capture());
        assertTrue(captor.getValue().getSqlSegment().contains("owner_id ="));
        assertTrue(captor.getValue().getParamNameValuePairs().containsValue(1L)); assertFalse(captor.getValue().getParamNameValuePairs().containsValue(13L));
    }
    @Test void houseScopeRequiresOwnershipAndActiveDates() {
        controller.houseIds(1L);
        ArgumentCaptor<LambdaQueryWrapper<BaseOwnerHouse>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(relations).selectList(captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertTrue(sql.contains("owner_id =")); assertTrue(sql.contains("relation_type =")); assertTrue(sql.contains("start_date <=")); assertTrue(sql.contains("end_date >="));
    }
    @Test void foreignRepairCannotBeEvaluated() {
        PropRepair other = new PropRepair(); other.setOwnerId(2L); other.setStatus(3); when(repairs.getById(99L)).thenReturn(other);
        assertEquals(404, assertThrows(BusinessException.class, () -> controller.evaluate(99L, new ResidentController.Evaluation(5, "test"))).getCode());
        verify(repairs, never()).evaluate(any(), any(), any());
    }
    @Test void invalidPhoneAndOversizedPagesAreRejected() {
        assertThrows(BusinessException.class, () -> controller.repair(new ResidentController.ServiceRequest(10L, "title", "content", 1, 1, "test", "123")));
        assertThrows(BusinessException.class, () -> controller.list("bills", 1, 1000)); verifyNoInteractions(repairs, bills);
    }
    @Test void nonOwnerCannotCallResidentControllerDirectly() {
        SysUser admin = new SysUser(); admin.setUserType(5); admin.setStatus(1); when(users.getById(13L)).thenReturn(admin);
        assertEquals(403, assertThrows(BusinessException.class, () -> controller.summary()).getCode()); verifyNoInteractions(owners);
    }
}
