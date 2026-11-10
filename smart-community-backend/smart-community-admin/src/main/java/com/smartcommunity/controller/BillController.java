package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.entity.FeeBill;
import com.smartcommunity.payment.PaymentResult;
import com.smartcommunity.service.BizDisplayService;
import com.smartcommunity.service.FeeBillPaymentService;
import com.smartcommunity.service.FeeBillService;
import com.smartcommunity.task.BillScheduledTask;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 账单管理。
 * <p>支付相关逻辑已下沉到 {@link FeeBillPaymentService}，经 {@code payment.*} 模拟/真实通道隔离，
 * 本 Controller 不再直接改 paidAmount / 写 FeePayment 冒充支付成功。
 */
@Tag(name = "账单管理")
@RestController
@RequestMapping("/api/v1/fee/bill")
@RequiredArgsConstructor
public class BillController {

    private final FeeBillService feeBillService;
    private final FeeBillPaymentService feeBillPaymentService;
    private final BizDisplayService bizDisplayService;
    private final BillScheduledTask billScheduledTask;

    @Operation(summary = "账单列表")
    @GetMapping("/list")
    public Result<Page<FeeBill>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long ownerId,
            @RequestParam(required = false) Long houseId,
            @RequestParam(required = false) String billPeriod,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<FeeBill> qw = new LambdaQueryWrapper<>();
        if (ownerId != null) qw.eq(FeeBill::getOwnerId, ownerId);
        if (houseId != null) qw.eq(FeeBill::getHouseId, houseId);
        if (billPeriod != null) qw.eq(FeeBill::getBillPeriod, billPeriod);
        if (status != null) qw.eq(FeeBill::getStatus, status);
        qw.orderByDesc(FeeBill::getCreateTime);
        Page<FeeBill> result = feeBillService.page(new Page<>(page, pageSize), qw);
        bizDisplayService.fill(result.getRecords(),
                FeeBill::getOwnerId, FeeBill::getHouseId,
                FeeBill::setOwnerName, FeeBill::setHouseNo);
        return Result.success(result);
    }

    @Operation(summary = "账单详情")
    @GetMapping("/{id}")
    public Result<FeeBill> getById(@PathVariable Long id) {
        FeeBill bill = feeBillService.getById(id);
        if (bill != null) {
            bizDisplayService.fill(java.util.List.of(bill),
                    FeeBill::getOwnerId, FeeBill::getHouseId,
                    FeeBill::setOwnerName, FeeBill::setHouseNo);
        }
        return Result.success(bill);
    }

    @Operation(summary = "生成账单（手动触发）")
    @PostMapping("/generate")
    public Result<Void> generate(@RequestBody Map<String, Object> params) {
        String period = (String) params.get("period");
        if (period == null || period.isEmpty()) {
            period = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
        }
        billScheduledTask.generateByPeriod(period);
        return Result.success();
    }

    @Operation(summary = "支付回调（经统一支付网关验通后再入账；mock 模式下为模拟回调）")
    @PostMapping("/callback")
    public Result<Map<String, Object>> callback(@RequestBody Map<String, Object> params) {
        PaymentResult result = feeBillPaymentService.handleCallback(params);
        return Result.success(toPayView(result));
    }

    @Operation(summary = "缴费（经统一支付网关；默认 mock 通道，非真实资金）")
    @PostMapping("/{id}/pay")
    public Result<Map<String, Object>> pay(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> params) {
        if (params == null) {
            params = Map.of();
        }
        BigDecimal amount = null;
        if (params.get("amount") != null) {
            amount = new BigDecimal(params.get("amount").toString());
        }
        Integer payMethod = null;
        if (params.get("payMethod") != null && !params.get("payMethod").toString().isBlank()) {
            payMethod = Integer.valueOf(params.get("payMethod").toString());
        }
        PaymentResult result = feeBillPaymentService.pay(id, amount, payMethod);
        String msg = result.isSimulated() ? "模拟缴费成功（非真实资金）" : "缴费成功";
        return Result.success(msg, toPayView(result));
    }

    @Operation(summary = "催缴账单")
    @PostMapping("/{id}/remind")
    public Result<Void> remind(@PathVariable Long id) {
        FeeBill bill = feeBillService.getById(id);
        if (bill == null) return Result.fail("账单不存在");
        // 实际催缴逻辑：发送短信/推送通知（尚未接入）
        return Result.success();
    }

    @Operation(summary = "作废账单")
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        FeeBill bill = feeBillService.getById(id);
        if (bill == null) return Result.fail("账单不存在");
        bill.setStatus(3); // 已作废
        feeBillService.updateById(bill);
        return Result.success();
    }

    private Map<String, Object> toPayView(PaymentResult result) {
        Map<String, Object> data = new HashMap<>();
        data.put("success", result.isSuccess());
        data.put("simulated", result.isSimulated());
        data.put("channel", result.getChannel() != null ? result.getChannel().name() : null);
        data.put("tradeNo", result.getTradeNo());
        data.put("amount", result.getAmount());
        data.put("payMethod", result.getPayMethod());
        data.put("paidAt", result.getPaidAt());
        data.put("message", result.getMessage());
        return data;
    }
}
