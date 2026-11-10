package com.smartcommunity.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartcommunity.entity.BaseHouse;
import com.smartcommunity.entity.FeeBill;
import com.smartcommunity.entity.FeeItem;
import com.smartcommunity.service.BaseHouseService;
import com.smartcommunity.service.FeeBillService;
import com.smartcommunity.service.FeeItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillScheduledTask {

    private final FeeItemService feeItemService;
    private final FeeBillService feeBillService;
    private final BaseHouseService houseService;

    /**
     * calcType: 1-按面积 2-按套 3-固定金额
     */
    BigDecimal calcAmount(FeeItem item, BaseHouse house) {
        BigDecimal unitPrice = item.getUnitPrice() == null ? BigDecimal.ZERO : item.getUnitPrice();
        Integer calcType = item.getCalcType() == null ? 2 : item.getCalcType();
        if (calcType != null && calcType == 1) {
            BigDecimal area = house.getArea() == null ? BigDecimal.ZERO : house.getArea();
            return unitPrice.multiply(area).setScale(2, RoundingMode.HALF_UP);
        }
        return unitPrice.setScale(2, RoundingMode.HALF_UP);
    }

    private boolean createBillIfAbsent(BaseHouse house, FeeItem item, String period,
                                       LocalDateTime start, LocalDateTime end) {
        long exists = feeBillService.count(new LambdaQueryWrapper<FeeBill>()
                .eq(FeeBill::getHouseId, house.getId())
                .eq(FeeBill::getItemId, item.getId())
                .eq(FeeBill::getBillPeriod, period));
        if (exists > 0) {
            return false;
        }

        BigDecimal amount = calcAmount(item, house);
        FeeBill bill = new FeeBill();
        bill.setBillNo("BL" + period.replace("-", "") + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        bill.setOwnerId(house.getOwnerId());
        bill.setHouseId(house.getId());
        bill.setItemId(item.getId());
        bill.setItemName(item.getItemName());
        bill.setBillType(item.getItemType());
        bill.setBillPeriod(period);
        if (start != null) {
            bill.setBillStartDate(start);
        }
        if (end != null) {
            bill.setBillEndDate(end);
        }
        bill.setAmount(amount);
        bill.setDiscountAmount(BigDecimal.ZERO);
        bill.setLateFee(BigDecimal.ZERO);
        bill.setPayableAmount(amount);
        bill.setPaidAmount(BigDecimal.ZERO);
        bill.setStatus(0);
        feeBillService.save(bill);
        return true;
    }

    @Scheduled(cron = "0 0 2 1 * ?")
    @Transactional
    public void generateMonthlyBills() {
        String period = LocalDate.now().minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));
        log.info("开始自动生成月度账单: period={}", period);
        try {
            int generated = doGenerate(period,
                    LocalDate.now().minusMonths(1).atStartOfDay(),
                    LocalDate.now().minusDays(1).atTime(23, 59, 59));
            log.info("月度账单生成完成: period={}, 生成 {} 条账单", period, generated);
        } catch (Exception e) {
            log.error("自动生成账单失败: period={}", period, e);
        }
    }

    @Transactional
    public void generateByPeriod(String period) {
        log.info("手动生成账单: period={}", period);
        int generated = doGenerate(period, null, null);
        log.info("手动账单生成完成: period={}, 生成 {} 条账单", period, generated);
    }

    private int doGenerate(String period, LocalDateTime start, LocalDateTime end) {
        List<FeeItem> activeItems = feeItemService.list(
                new LambdaQueryWrapper<FeeItem>().eq(FeeItem::getStatus, 1));
        if (activeItems.isEmpty()) {
            log.info("无启用的费用项目，跳过账单生成");
            return 0;
        }
        List<BaseHouse> houses = houseService.list();
        int generated = 0;
        for (BaseHouse house : houses) {
            for (FeeItem item : activeItems) {
                if (createBillIfAbsent(house, item, period, start, end)) {
                    generated++;
                }
            }
        }
        return generated;
    }
}
