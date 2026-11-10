package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.entity.FeeBill;
import com.smartcommunity.service.BizDisplayService;
import com.smartcommunity.service.FeeBillService;
import com.smartcommunity.service.FeePaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "财务报表")
@RestController
@RequestMapping("/api/v1/fee/report")
@RequiredArgsConstructor
public class FeeReportController {

    private final FeeBillService feeBillService;
    private final FeePaymentService feePaymentService;
    private final BizDisplayService bizDisplayService;

    @Operation(summary = "费用汇总报表")
    @GetMapping("/summary")
    public Result<Map<String, Object>> summary(@RequestParam(required = false) String billPeriod) {
        LambdaQueryWrapper<FeeBill> qw = new LambdaQueryWrapper<>();
        if (billPeriod != null) qw.eq(FeeBill::getBillPeriod, billPeriod);

        List<FeeBill> bills = feeBillService.list(qw);
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;

        for (FeeBill bill : bills) {
            if (bill.getPayableAmount() != null) {
                totalAmount = totalAmount.add(bill.getPayableAmount());
            }
            totalPaid = totalPaid.add(bill.getPaidAmount() != null ? bill.getPaidAmount() : BigDecimal.ZERO);
        }

        long paidCount = bills.stream().filter(b -> b.getStatus() != null && b.getStatus() == 2).count();
        long unpaidCount = bills.size() - paidCount;

        // 与首页口径一致：按金额计算收缴率
        BigDecimal collectionRate = totalAmount.compareTo(BigDecimal.ZERO) > 0
                ? totalPaid.multiply(BigDecimal.valueOf(100))
                    .divide(totalAmount, 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Map<String, Object> result = new HashMap<>();
        result.put("totalAmount", totalAmount);
        result.put("totalPaid", totalPaid);
        result.put("totalUnpaid", totalAmount.subtract(totalPaid));
        result.put("billCount", bills.size());
        result.put("paidCount", paidCount);
        result.put("unpaidCount", unpaidCount);
        result.put("collectionRate", collectionRate);
        return Result.success(result);
    }

    @Operation(summary = "欠费报表")
    @GetMapping("/arrears")
    public Result<Map<String, Object>> arrears(@RequestParam(required = false) Long houseId) {
        LambdaQueryWrapper<FeeBill> qw = new LambdaQueryWrapper<>();
        qw.ne(FeeBill::getStatus, 2).ne(FeeBill::getStatus, 3); // 未缴清且未作废
        if (houseId != null) qw.eq(FeeBill::getHouseId, houseId);

        List<FeeBill> arrearsBills = feeBillService.list(qw);
        bizDisplayService.fill(arrearsBills,
                FeeBill::getOwnerId, FeeBill::getHouseId,
                FeeBill::setOwnerName, FeeBill::setHouseNo);

        BigDecimal totalArrears = arrearsBills.stream()
                .map(b -> {
                    BigDecimal payable = b.getPayableAmount() != null ? b.getPayableAmount() : BigDecimal.ZERO;
                    BigDecimal paid = b.getPaidAmount() != null ? b.getPaidAmount() : BigDecimal.ZERO;
                    return payable.subtract(paid);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> result = new HashMap<>();
        result.put("arrearsCount", arrearsBills.size());
        result.put("totalArrears", totalArrears);
        result.put("arrearsBills", arrearsBills);
        return Result.success(result);
    }

    @Operation(summary = "导出报表")
    @GetMapping("/export")
    public Result<Void> export(@RequestParam(required = false) String billPeriod) {
        // 演示阶段未实现真实文件导出，明确失败避免前端假成功
        return Result.fail("导出功能暂未开放，请使用页面数据或后续版本导出 CSV/Excel");
    }
}
