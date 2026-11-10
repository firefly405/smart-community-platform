package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.entity.FeeDeposit;
import com.smartcommunity.entity.FeeRefund;
import com.smartcommunity.service.BizDisplayService;
import com.smartcommunity.service.FeeDepositService;
import com.smartcommunity.service.FeeRefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Tag(name = "押金管理")
@RestController
@RequestMapping("/api/v1/fee/deposit")
@RequiredArgsConstructor
public class DepositController {

    private final FeeDepositService feeDepositService;
    private final FeeRefundService feeRefundService;
    private final BizDisplayService bizDisplayService;

    @Operation(summary = "押金列表")
    @GetMapping("/list")
    public Result<Page<FeeDeposit>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long ownerId,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<FeeDeposit> qw = new LambdaQueryWrapper<>();
        if (ownerId != null) qw.eq(FeeDeposit::getOwnerId, ownerId);
        if (status != null) qw.eq(FeeDeposit::getStatus, status);
        qw.orderByDesc(FeeDeposit::getCreateTime);
        Page<FeeDeposit> result = feeDepositService.page(new Page<>(page, pageSize), qw);
        bizDisplayService.fill(result.getRecords(),
                FeeDeposit::getOwnerId, FeeDeposit::getHouseId,
                FeeDeposit::setOwnerName, FeeDeposit::setHouseNo);
        return Result.success(result);
    }

    @Operation(summary = "新增押金")
    @PostMapping
    public Result<Void> create(@RequestBody FeeDeposit deposit) {
        deposit.setDepositNo("DEP" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        deposit.setReceiveTime(LocalDateTime.now());
        deposit.setStatus(0); // 已收
        deposit.setRefundedAmount(BigDecimal.ZERO);
        feeDepositService.save(deposit);
        return Result.success();
    }

    @Operation(summary = "押金退款（同步写退款单）")
    @PutMapping("/{id}/refund")
    @Transactional
    public Result<Void> refund(@PathVariable Long id) {
        FeeDeposit deposit = feeDepositService.getById(id);
        if (deposit == null) return Result.fail("押金记录不存在");
        if (deposit.getStatus() != null && deposit.getStatus() == 2) {
            return Result.fail("押金已全额退还");
        }

        BigDecimal amount = deposit.getAmount() == null ? BigDecimal.ZERO : deposit.getAmount();
        BigDecimal already = deposit.getRefundedAmount() == null ? BigDecimal.ZERO : deposit.getRefundedAmount();
        BigDecimal remain = amount.subtract(already);
        if (remain.compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail("无可退押金");
        }

        deposit.setStatus(2); // 已全退
        deposit.setRefundedAmount(amount);
        feeDepositService.updateById(deposit);

        // 联动写已退款退款单，便于退款列表/报表流通
        FeeRefund refund = new FeeRefund();
        refund.setRefundNo("REF" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        refund.setOwnerId(deposit.getOwnerId());
        refund.setDepositId(deposit.getId());
        refund.setRefundType(1); // 押金退款
        refund.setAmount(remain);
        refund.setStatus(2); // 已退款
        refund.setApplyReason("押金页直接退款");
        refund.setApproveTime(LocalDateTime.now());
        refund.setRefundTime(LocalDateTime.now());
        feeRefundService.save(refund);

        return Result.success();
    }
}
