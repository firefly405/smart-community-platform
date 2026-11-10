package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.entity.FeeBill;
import com.smartcommunity.entity.FeeDeposit;
import com.smartcommunity.entity.FeePayment;
import com.smartcommunity.entity.FeeRefund;
import com.smartcommunity.service.BizDisplayService;
import com.smartcommunity.service.FeeBillService;
import com.smartcommunity.service.FeeDepositService;
import com.smartcommunity.service.FeePaymentService;
import com.smartcommunity.service.FeeRefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Tag(name = "退款管理")
@RestController
@RequestMapping("/api/v1/fee/refund")
@RequiredArgsConstructor
public class RefundController {

    private final FeeRefundService feeRefundService;
    private final FeeBillService feeBillService;
    private final FeePaymentService feePaymentService;
    private final FeeDepositService feeDepositService;
    private final BizDisplayService bizDisplayService;

    @Operation(summary = "退款申请列表")
    @GetMapping("/list")
    public Result<Page<FeeRefund>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<FeeRefund> qw = new LambdaQueryWrapper<>();
        if (status != null) qw.eq(FeeRefund::getStatus, status);
        qw.orderByDesc(FeeRefund::getCreateTime);
        Page<FeeRefund> result = feeRefundService.page(new Page<>(page, pageSize), qw);
        bizDisplayService.fillOwnerOnly(result.getRecords(), FeeRefund::getOwnerId, FeeRefund::setOwnerName);
        return Result.success(result);
    }

    @Operation(summary = "申请退款")
    @PostMapping
    public Result<Void> apply(@RequestBody FeeRefund refund) {
        refund.setRefundNo("REF" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        refund.setStatus(0); // 待审批
        feeRefundService.save(refund);
        return Result.success();
    }

    @Operation(summary = "审批退款（通过时冲正账单/联动押金）")
    @PutMapping("/{id}/approve")
    @Transactional
    public Result<Void> approve(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        FeeRefund refund = feeRefundService.getById(id);
        if (refund == null) return Result.fail("退款申请不存在");
        if (refund.getStatus() != null && refund.getStatus() != 0) {
            return Result.fail("该退款申请已处理");
        }

        boolean approved = Boolean.TRUE.equals(params.get("approved"));
        refund.setApproverId(params.get("approverId") != null ? Long.valueOf(params.get("approverId").toString()) : null);
        refund.setApproveTime(LocalDateTime.now());

        if (!approved) {
            refund.setStatus(3); // 已驳回
            refund.setRejectReason(params.get("remark") != null ? params.get("remark").toString() : null);
            feeRefundService.updateById(refund);
            return Result.success();
        }

        BigDecimal refundAmount = refund.getAmount() == null ? BigDecimal.ZERO : refund.getAmount();
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail("退款金额无效");
        }

        // 1) 关联账单冲正
        if (refund.getBillId() != null) {
            FeeBill bill = feeBillService.getById(refund.getBillId());
            if (bill == null) return Result.fail("关联账单不存在");
            if (bill.getStatus() != null && bill.getStatus() == 3) {
                return Result.fail("账单已作废，无法退款");
            }
            BigDecimal paid = bill.getPaidAmount() == null ? BigDecimal.ZERO : bill.getPaidAmount();
            if (refundAmount.compareTo(paid) > 0) {
                return Result.fail("退款金额不能大于账单已付金额");
            }
            BigDecimal newPaid = paid.subtract(refundAmount);
            bill.setPaidAmount(newPaid);
            BigDecimal payable = bill.getPayableAmount() == null ? BigDecimal.ZERO : bill.getPayableAmount();
            if (newPaid.compareTo(BigDecimal.ZERO) <= 0) {
                bill.setStatus(0);
                bill.setPayTime(null);
            } else if (newPaid.compareTo(payable) < 0) {
                bill.setStatus(1);
            } else {
                bill.setStatus(2);
            }
            feeBillService.updateById(bill);

            // 负向缴费流水（payStatus=2 表示退款）
            FeePayment payment = new FeePayment();
            payment.setPaymentNo("RFD" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
            payment.setBillId(bill.getId());
            payment.setOwnerId(bill.getOwnerId());
            payment.setHouseId(bill.getHouseId());
            payment.setPayAmount(refundAmount.negate());
            payment.setPayMethod(null);
            payment.setPayStatus(2);
            payment.setPayTime(LocalDateTime.now());
            payment.setRemark("退款冲正 REF:" + refund.getRefundNo());
            feePaymentService.save(payment);
        }

        // 2) 关联押金联动
        if (refund.getDepositId() != null) {
            FeeDeposit deposit = feeDepositService.getById(refund.getDepositId());
            if (deposit == null) return Result.fail("关联押金不存在");
            BigDecimal already = deposit.getRefundedAmount() == null ? BigDecimal.ZERO : deposit.getRefundedAmount();
            BigDecimal depAmount = deposit.getAmount() == null ? BigDecimal.ZERO : deposit.getAmount();
            BigDecimal remain = depAmount.subtract(already);
            if (refundAmount.compareTo(remain) > 0) {
                return Result.fail("退款金额不能大于押金可退余额");
            }
            BigDecimal newRefunded = already.add(refundAmount);
            deposit.setRefundedAmount(newRefunded);
            if (newRefunded.compareTo(depAmount) >= 0) {
                deposit.setStatus(2); // 已全退
            } else {
                deposit.setStatus(1); // 部分退
            }
            feeDepositService.updateById(deposit);
        }

        refund.setStatus(2); // 已退款
        refund.setRefundTime(LocalDateTime.now());
        feeRefundService.updateById(refund);
        return Result.success();
    }
}
