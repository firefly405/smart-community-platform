package com.smartcommunity.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcommunity.entity.*;
import com.smartcommunity.mapper.SysQaLogMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResidentQaServiceTest {
    private final ObjectProvider<ChatModel> models = mock(ObjectProvider.class);
    private final SysUserService users = mock(SysUserService.class);
    private final BaseOwnerService owners = mock(BaseOwnerService.class);
    private final FeeBillService bills = mock(FeeBillService.class);
    private final PropRepairService repairs = mock(PropRepairService.class);
    private final PropNoticeService notices = mock(PropNoticeService.class);
    private final LifeActivityService activities = mock(LifeActivityService.class);
    private final SocialPhoneService phones = mock(SocialPhoneService.class);
    private final BaseParkingService parking = mock(BaseParkingService.class);
    private final SysQaLogMapper logs = mock(SysQaLogMapper.class);
    private final ResidentQaService service = new ResidentQaService(models, users, owners, bills, repairs, notices, activities, phones, parking, logs, new ObjectMapper());
    @BeforeAll static void tables() { for (Class<?> type : List.of(BaseOwner.class, FeeBill.class, SysQaLog.class, PropRepair.class, PropNotice.class, LifeActivity.class, SocialPhone.class, BaseParking.class)) TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), type); }
    @BeforeEach void setup() {
        SysUser user = new SysUser(); user.setId(13L); user.setUserType(1); user.setStatus(1); when(users.getById(13L)).thenReturn(user);
        when(bills.page(any(Page.class), any())).thenReturn(new Page<FeeBill>()); when(repairs.page(any(Page.class), any())).thenReturn(new Page<PropRepair>());
        when(notices.page(any(Page.class), any())).thenReturn(new Page<PropNotice>()); when(activities.page(any(Page.class), any())).thenReturn(new Page<LifeActivity>());
        when(phones.page(any(Page.class), any())).thenReturn(new Page<SocialPhone>()); when(parking.page(any(Page.class), any())).thenReturn(new Page<BaseParking>());
    }
    @Test void privateContextUsesBoundOwnerNotAccountId() {
        BaseOwner owner = new BaseOwner(); owner.setId(1L); when(owners.getOne(any())).thenReturn(owner); service.context(13L);
        ArgumentCaptor<LambdaQueryWrapper<FeeBill>> capture = ArgumentCaptor.forClass(LambdaQueryWrapper.class); verify(bills).page(any(Page.class), capture.capture());
        assertTrue(capture.getValue().getSqlSegment().contains("owner_id =")); assertTrue(capture.getValue().getParamNameValuePairs().containsValue(1L)); assertFalse(capture.getValue().getParamNameValuePairs().containsValue(13L));
        assertFalse(capture.getValue().getSqlSelect().contains("owner_id"));
    }
    @Test void unboundUserNeverQueriesAnyPrivateData() { service.context(13L); verifyNoInteractions(bills, repairs); }
    @Test void historyAlwaysIncludesAccountAndResidentScopeFilters() {
        service.history(13L, 1); ArgumentCaptor<LambdaQueryWrapper<SysQaLog>> capture = ArgumentCaptor.forClass(LambdaQueryWrapper.class); verify(logs).selectPage(any(Page.class), capture.capture());
        assertTrue(capture.getValue().getSqlSegment().contains("user_id =")); assertTrue(capture.getValue().getParamNameValuePairs().containsValue(13L)); assertTrue(capture.getValue().getParamNameValuePairs().containsValue("resident"));
    }
    @Test void missingModelReturnsResidentFriendlyMessageWithoutManagementInstructions() { assertEquals("智能问答暂未启用，请联系物业开通服务。", service.ask("Hello", 13L).getAnswer()); verifyNoInteractions(logs); }
    @Test void modelReceivesOnlyScopedSnapshotAndHistoryBelongsToCaller() {
        ChatModel model = mock(ChatModel.class); when(models.getIfAvailable()).thenReturn(model);
        when(model.call(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(new org.springframework.ai.chat.model.ChatResponse(List.of(new org.springframework.ai.chat.model.Generation(new org.springframework.ai.chat.messages.AssistantMessage("请联系物业")))));
        assertEquals("请联系物业", service.ask("物业电话？", 13L).getAnswer());
        ArgumentCaptor<org.springframework.ai.chat.prompt.Prompt> prompt = ArgumentCaptor.forClass(org.springframework.ai.chat.prompt.Prompt.class);
        verify(model).call(prompt.capture()); assertEquals(3, prompt.getValue().getInstructions().size());
        assertTrue(prompt.getValue().getInstructions().get(0).getContent().contains("拒绝查询"));
        assertFalse(prompt.getValue().getInstructions().get(1).getContent().contains("本人未缴清账单"));
        ArgumentCaptor<SysQaLog> entry = ArgumentCaptor.forClass(SysQaLog.class); verify(logs).insert(entry.capture());
        assertEquals(13L, entry.getValue().getUserId()); assertEquals("[]", entry.getValue().getSources()); assertEquals("resident", entry.getValue().getRemark());
    }
    @Test void historyFailureDoesNotDiscardSuccessfulModelAnswer() {
        ChatModel model = mock(ChatModel.class); when(models.getIfAvailable()).thenReturn(model);
        when(model.call(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(new org.springframework.ai.chat.model.ChatResponse(List.of(new org.springframework.ai.chat.model.Generation(new org.springframework.ai.chat.messages.AssistantMessage("有效回答")))));
        doThrow(new org.springframework.dao.DataIntegrityViolationException("test")).when(logs).insert(any(SysQaLog.class));
        assertEquals("有效回答", service.ask("你好", 13L).getAnswer());
    }
}
