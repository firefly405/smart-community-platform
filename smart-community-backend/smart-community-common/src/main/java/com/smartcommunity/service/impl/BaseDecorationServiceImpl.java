package com.smartcommunity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartcommunity.common.exception.BusinessException;
import com.smartcommunity.entity.BaseDecoration;
import com.smartcommunity.entity.FeeDeposit;
import com.smartcommunity.mapper.BaseDecorationMapper;
import com.smartcommunity.service.BaseDecorationService;
import com.smartcommunity.service.FeeDepositService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BaseDecorationServiceImpl extends ServiceImpl<BaseDecorationMapper, BaseDecoration> implements BaseDecorationService {

    private final FeeDepositService feeDepositService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, Integer status, String checkResult) {
        BaseDecoration deco = getById(id);
        if (deco == null) {
            throw new BusinessException(404, "装修申请不存在");
        }
        deco.setStatus(status);
        if (status != null && status == 3) {
            deco.setActualEndDate(LocalDate.now());
        }
        if (checkResult != null) {
            deco.setCheckResult(checkResult);
        }
        updateById(deco);

        // 审批通过且有押金金额时，自动创建装修押金，打通 装修→押金 演示闭环
        if (status != null && status == 1) {
            createDecorationDepositIfNeeded(deco);
        }
    }

    private void createDecorationDepositIfNeeded(BaseDecoration deco) {
        if (deco.getDepositAmount() == null || deco.getDepositAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        String mark = buildDepositRemark(deco);
        long exists = feeDepositService.count(new LambdaQueryWrapper<FeeDeposit>()
                .eq(FeeDeposit::getRemark, mark)
                .eq(FeeDeposit::getDepositType, 1));
        if (exists > 0) {
            return;
        }

        FeeDeposit deposit = new FeeDeposit();
        deposit.setDepositNo("DEP" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        deposit.setOwnerId(deco.getOwnerId());
        deposit.setHouseId(deco.getHouseId());
        deposit.setDepositType(1); // 装修押金
        deposit.setAmount(deco.getDepositAmount());
        deposit.setRefundedAmount(BigDecimal.ZERO);
        deposit.setStatus(0); // 已收
        deposit.setReceiveTime(LocalDateTime.now());
        deposit.setRemark(mark);
        feeDepositService.save(deposit);
    }

    private String buildDepositRemark(BaseDecoration deco) {
        String no = deco.getDecorationNo();
        if (no == null || no.isBlank()) {
            no = String.valueOf(deco.getId());
        }
        return "装修单:" + no;
    }
}
