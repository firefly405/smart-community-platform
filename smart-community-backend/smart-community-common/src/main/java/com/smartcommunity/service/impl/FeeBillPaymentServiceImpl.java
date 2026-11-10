package com.smartcommunity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartcommunity.common.exception.BusinessException;
import com.smartcommunity.entity.FeeBill;
import com.smartcommunity.entity.FeePayment;
import com.smartcommunity.payment.PaymentBizType;
import com.smartcommunity.payment.PaymentGatewayRouter;
import com.smartcommunity.payment.PaymentRequest;
import com.smartcommunity.payment.PaymentResult;
import com.smartcommunity.service.FeeBillPaymentService;
import com.smartcommunity.service.FeeBillService;
import com.smartcommunity.service.FeePaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 账单支付编排实现。
 * <p>支付成功判定只来自 {@link PaymentGatewayRouter}；本类负责业务落账。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeeBillPaymentServiceImpl implements FeeBillPaymentService {

    private final FeeBillService feeBillService;
    private final FeePaymentService feePaymentService;
    private final PaymentGatewayRouter paymentGatewayRouter;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentResult pay(Long billId, BigDecimal amount, Integer payMethod) {
        FeeBill bill = feeBillService.getById(billId);
        if (bill == null) {
            throw new BusinessException(404, "账单不存在");
        }
        if (bill.getStatus() != null && bill.getStatus() == 3) {
            throw new BusinessException(400, "账单已作废，无法缴费");
        }
        if (bill.getStatus() != null && bill.getStatus() == 2) {
            throw new BusinessException(400, "账单已缴清");
        }

        BigDecimal payable = nvl(bill.getPayableAmount());
        BigDecimal paid = nvl(bill.getPaidAmount());
        BigDecimal remain = payable.subtract(paid);
        if (remain.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "账单无剩余应缴金额");
        }

        BigDecimal payAmount = amount != null ? amount : remain;
        if (payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "支付金额必须大于 0");
        }
        if (payAmount.compareTo(remain) > 0) {
            throw new BusinessException(400, "支付金额不能超过剩余应缴 " + remain);
        }

        PaymentResult result = paymentGatewayRouter.charge(PaymentRequest.builder()
                .bizType(PaymentBizType.FEE_BILL)
                .bizId(bill.getId())
                .bizNo(bill.getBillNo())
                .amount(payAmount)
                .payMethod(payMethod)
                .remark(paymentGatewayRouter.isSimulated() ? "mock-pay" : "pay")
                .build());

        if (!result.isSuccess()) {
            throw new BusinessException(400, result.getMessage() != null ? result.getMessage() : "支付失败");
        }

        applyPayment(bill, result.getAmount() != null ? result.getAmount() : payAmount,
                result.getPayMethod() != null ? result.getPayMethod() : payMethod,
                result.getTradeNo(),
                result.isSimulated());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentResult handleCallback(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            throw new BusinessException(400, "回调参数为空");
        }

        PaymentResult gatewayResult = paymentGatewayRouter.handleCallback(payload);
        if (!gatewayResult.isSuccess()) {
            throw new BusinessException(400,
                    gatewayResult.getMessage() != null ? gatewayResult.getMessage() : "回调校验失败");
        }

        // 定位账单：优先 billId，其次 trade_no 关联已有 payTradeNo，再次 billNo
        FeeBill bill = resolveBill(payload, gatewayResult.getTradeNo());
        if (bill == null) {
            throw new BusinessException(404, "回调对应账单不存在");
        }
        if (bill.getStatus() != null && (bill.getStatus() == 2 || bill.getStatus() == 3)) {
            // 已缴清/作废：幂等成功
            return gatewayResult;
        }

        BigDecimal payAmount = gatewayResult.getAmount();
        if (payAmount == null) {
            BigDecimal payable = nvl(bill.getPayableAmount());
            BigDecimal paid = nvl(bill.getPaidAmount());
            payAmount = payable.subtract(paid);
        }
        if (payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return gatewayResult;
        }

        applyPayment(bill, payAmount,
                gatewayResult.getPayMethod(),
                gatewayResult.getTradeNo(),
                gatewayResult.isSimulated());
        return gatewayResult;
    }

    private void applyPayment(FeeBill bill, BigDecimal payAmount, Integer payMethod,
                              String tradeNo, boolean simulated) {
        BigDecimal newPaid = nvl(bill.getPaidAmount()).add(payAmount);
        bill.setPaidAmount(newPaid);
        BigDecimal payable = nvl(bill.getPayableAmount());
        if (newPaid.compareTo(payable) >= 0) {
            bill.setStatus(2); // 已缴清
        } else {
            bill.setStatus(1); // 部分缴费
        }
        bill.setPayTime(LocalDateTime.now());
        bill.setPayMethod(payMethod);
        bill.setPayTradeNo(tradeNo);
        if (simulated) {
            String remark = bill.getRemark();
            String tag = "[MOCK-PAY]";
            if (remark == null || !remark.contains(tag)) {
                bill.setRemark(remark == null || remark.isBlank() ? tag : remark + " " + tag);
            }
        }
        feeBillService.updateById(bill);

        FeePayment payment = new FeePayment();
        payment.setPaymentNo("PAY" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        payment.setBillId(bill.getId());
        payment.setOwnerId(bill.getOwnerId());
        payment.setHouseId(bill.getHouseId());
        payment.setPayAmount(payAmount);
        payment.setPayMethod(payMethod);
        payment.setPayTradeNo(tradeNo);
        payment.setPayStatus(1);
        payment.setPayTime(LocalDateTime.now());
        payment.setRemark(simulated ? "模拟支付入账（非真实资金）" : "支付入账");
        feePaymentService.save(payment);

        log.info("[FEE-BILL-PAY] billId={} amount={} tradeNo={} simulated={}",
                bill.getId(), payAmount, tradeNo, simulated);
    }

    private FeeBill resolveBill(Map<String, Object> payload, String tradeNo) {
        Object billIdObj = first(payload, "billId", "bill_id", "bizId");
        if (billIdObj != null) {
            try {
                return feeBillService.getById(Long.valueOf(billIdObj.toString()));
            } catch (Exception ignored) {
                // fall through
            }
        }
        Object billNoObj = first(payload, "billNo", "bill_no", "bizNo");
        if (billNoObj != null) {
            FeeBill byNo = feeBillService.getOne(new LambdaQueryWrapper<FeeBill>()
                    .eq(FeeBill::getBillNo, billNoObj.toString())
                    .last("LIMIT 1"));
            if (byNo != null) {
                return byNo;
            }
        }
        if (tradeNo != null && !tradeNo.isBlank()) {
            return feeBillService.getOne(new LambdaQueryWrapper<FeeBill>()
                    .eq(FeeBill::getPayTradeNo, tradeNo)
                    .last("LIMIT 1"));
        }
        return null;
    }

    private static Object first(Map<String, Object> payload, String... keys) {
        for (String k : keys) {
            if (payload.containsKey(k) && payload.get(k) != null) {
                return payload.get(k);
            }
        }
        return null;
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
