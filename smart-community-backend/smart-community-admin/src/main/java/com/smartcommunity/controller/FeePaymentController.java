package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.entity.FeePayment;
import com.smartcommunity.service.BizDisplayService;
import com.smartcommunity.service.FeePaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "缴费记录管理")
@RestController
@RequestMapping("/api/v1/fee/payment")
@RequiredArgsConstructor
public class FeePaymentController {

    private final FeePaymentService feePaymentService;
    private final BizDisplayService bizDisplayService;

    @Operation(summary = "缴费记录列表")
    @GetMapping("/list")
    public Result<Page<FeePayment>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long billId,
            @RequestParam(required = false) Long ownerId,
            @RequestParam(required = false) Long houseId) {
        LambdaQueryWrapper<FeePayment> qw = new LambdaQueryWrapper<>();
        if (billId != null) qw.eq(FeePayment::getBillId, billId);
        if (ownerId != null) qw.eq(FeePayment::getOwnerId, ownerId);
        if (houseId != null) qw.eq(FeePayment::getHouseId, houseId);
        qw.orderByDesc(FeePayment::getPayTime);
        Page<FeePayment> result = feePaymentService.page(new Page<>(page, pageSize), qw);
        bizDisplayService.fill(result.getRecords(),
                FeePayment::getOwnerId, FeePayment::getHouseId,
                FeePayment::setOwnerName, FeePayment::setHouseNo);
        return Result.success(result);
    }
}
