package com.smartcommunity.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.smartcommunity.entity.LifeExpress;
import com.smartcommunity.mapper.LifeExpressMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
@Description("查询业主的快递信息：待取件、已取件、取件码、货架位置")
public class ExpressQueryTool implements Function<ExpressQueryTool.Request, ExpressQueryTool.Response> {

    private final LifeExpressMapper expressMapper;

    public record Request(
            @JsonProperty(required = true)
            @JsonPropertyDescription("业主ID")
            Long ownerId,
            @JsonPropertyDescription("状态筛选: PENDING=待取件, PICKED=已取件, DELAYED=滞留, ALL=全部")
            String statusFilter) {}

    public record Response(int totalCount, long pendingCount, List<ExpressInfo> expressList) {
        public record ExpressInfo(String company, String trackingNo, String pickupCode,
                                  String shelfNo, String status, String arriveTime, String pickupTime) {}
    }

    @Override
    public Response apply(Request req) {
        LambdaQueryWrapper<LifeExpress> qw = new LambdaQueryWrapper<>();
        qw.eq(LifeExpress::getReceiverId, req.ownerId());
        String sf = req.statusFilter();
        if (sf != null && !"ALL".equals(sf)) {
            int sc;
            if ("PENDING".equals(sf)) { sc = 0; }
            else if ("PICKED".equals(sf)) { sc = 1; }
            else if ("DELAYED".equals(sf)) { sc = 2; }
            else { sc = -1; }
            if (sc >= 0) qw.eq(LifeExpress::getStatus, sc);
        }
        qw.orderByDesc(LifeExpress::getArriveTime);
        List<LifeExpress> list = expressMapper.selectList(qw);
        List<Response.ExpressInfo> el = list.stream().map(e -> {
            int expressStatusVal = e.getStatus() != null ? e.getStatus() : 0;
            String st;
            if (expressStatusVal == 0) { st = "待取件"; }
            else if (expressStatusVal == 1) { st = "已取件"; }
            else if (expressStatusVal == 2) { st = "滞留"; }
            else if (expressStatusVal == 3) { st = "已退回"; }
            else { st = "未知"; }
            return new Response.ExpressInfo(e.getExpressCompany(), e.getTrackingNo(), e.getPickupCode(), e.getShelfNo(), st, e.getArriveTime() != null ? e.getArriveTime().toString() : "", e.getPickupTime() != null ? e.getPickupTime().toString() : null);
        }).toList();
        long pending = list.stream().filter(e -> e.getStatus() == 0).count();
        return new Response(list.size(), pending, el);
    }
}
