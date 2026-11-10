package com.smartcommunity.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.smartcommunity.entity.FeeBill;
import com.smartcommunity.entity.FeeItem;
import com.smartcommunity.mapper.FeeBillMapper;
import com.smartcommunity.mapper.FeeItemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
@Description("查询业主的物业费/停车费/水电费账单：未缴、已缴、欠费金额、费用明细")
public class BillQueryTool implements Function<BillQueryTool.Request, BillQueryTool.Response> {

    private final FeeBillMapper billMapper;
    private final FeeItemMapper itemMapper;

    public record Request(
            @JsonProperty(required = true)
            @JsonPropertyDescription("业主ID（关联到owner表）")
            Long ownerId,
            @JsonPropertyDescription("查询类型: UNPAID=未缴账单, PAID=已缴账单, ALL=全部")
            String queryType) {}

    public record Response(int totalCount, BigDecimal totalUnpaid, List<BillInfo> bills) {
        public record BillInfo(String billNo, String period, String itemName,
                               BigDecimal payableAmount, BigDecimal paidAmount, String status) {}
    }

    @Override
    public Response apply(Request req) {
        Long ownerId = req.ownerId();
        String queryType = req.queryType();

        LambdaQueryWrapper<FeeBill> qw = new LambdaQueryWrapper<>();
        qw.eq(FeeBill::getOwnerId, ownerId);
        if ("UNPAID".equals(queryType)) qw.eq(FeeBill::getStatus, 0);
        else if ("PAID".equals(queryType)) qw.in(FeeBill::getStatus, 1, 2);
        qw.orderByDesc(FeeBill::getBillPeriod);

        List<FeeBill> bills = billMapper.selectList(qw);
        BigDecimal totalUnpaid = bills.stream()
                .filter(b -> b.getStatus() == 0 || b.getStatus() == 1)
                .map(b -> b.getPayableAmount().subtract(b.getPaidAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Response.BillInfo> list = bills.stream()
                .map(b -> {
                    FeeItem item = itemMapper.selectById(b.getItemId());
                    Integer stInt = b.getStatus();
                    String statusText;
                    if (stInt == null) { statusText = "未知"; }
                    else if (stInt == 0) { statusText = "未缴费"; }
                    else if (stInt == 1) { statusText = "部分缴费"; }
                    else if (stInt == 2) { statusText = "已缴清"; }
                    else if (stInt == 3) { statusText = "已作废"; }
                    else { statusText = "未知"; }
                    return new Response.BillInfo(b.getBillNo(), b.getBillPeriod(),
                            item != null ? item.getItemName() : "",
                            b.getPayableAmount(), b.getPaidAmount(), statusText);
                }).toList();

        return new Response(bills.size(), totalUnpaid, list);
    }
}
