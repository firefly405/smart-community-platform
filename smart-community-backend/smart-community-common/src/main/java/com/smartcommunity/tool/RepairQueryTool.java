package com.smartcommunity.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.smartcommunity.entity.PropRepair;
import com.smartcommunity.entity.PropRepairItem;
import com.smartcommunity.mapper.PropRepairMapper;
import com.smartcommunity.mapper.PropRepairItemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
@Description("查询业主的报修工单：进度状态、处理人、评价、费用明细。可查全部或指定状态")
public class RepairQueryTool implements Function<RepairQueryTool.Request, RepairQueryTool.Response> {

    private final PropRepairMapper repairMapper;
    private final PropRepairItemMapper repairItemMapper;

    public record Request(
            @JsonProperty(required = true)
            @JsonPropertyDescription("业主ID")
            Long ownerId,
            @JsonPropertyDescription("状态筛选: PENDING=待处理, ASSIGNED=已派单, PROCESSING=处理中, COMPLETED=已完成, CANCELLED=已取消, ALL=全部")
            String statusFilter) {}

    public record Response(int totalCount, long pendingCount, long completedCount, List<RepairInfo> repairs) {
        public record RepairInfo(String repairNo, String title, String type, String status,
                                 String handlerName, Integer evaluateScore,
                                 BigDecimal totalCost, String createTime) {}
    }

    @Override
    public Response apply(Request req) {
        Long ownerId = req.ownerId();
        String sf = req.statusFilter();

        LambdaQueryWrapper<PropRepair> qw = new LambdaQueryWrapper<>();
        qw.eq(PropRepair::getOwnerId, ownerId);
        if (sf != null && !"ALL".equals(sf)) {
            int sc;
            if ("PENDING".equals(sf)) { sc = 0; }
            else if ("ASSIGNED".equals(sf)) { sc = 1; }
            else if ("PROCESSING".equals(sf)) { sc = 2; }
            else if ("COMPLETED".equals(sf)) { sc = 3; }
            else if ("CANCELLED".equals(sf)) { sc = 4; }
            else { sc = -1; }
            if (sc >= 0) qw.eq(PropRepair::getStatus, sc);
        }
        qw.orderByDesc(PropRepair::getId);

        List<PropRepair> repairs = repairMapper.selectList(qw);
        List<Response.RepairInfo> list = repairs.stream().map(r -> {
            Integer repairStatus = r.getStatus();
            String st;
            if (repairStatus == null) { st = "未知"; }
            else if (repairStatus == 0) { st = "待处理"; }
            else if (repairStatus == 1) { st = "已派单"; }
            else if (repairStatus == 2) { st = "处理中"; }
            else if (repairStatus == 3) { st = "已完成"; }
            else if (repairStatus == 4) { st = "已取消"; }
            else { st = "未知"; }
            int repairTypeVal = r.getRepairType() != null ? r.getRepairType() : 1;
            String tt;
            if (repairTypeVal == 1) { tt = "水电"; }
            else if (repairTypeVal == 2) { tt = "门窗"; }
            else if (repairTypeVal == 3) { tt = "墙面"; }
            else if (repairTypeVal == 4) { tt = "地面"; }
            else if (repairTypeVal == 5) { tt = "公共设施"; }
            else if (repairTypeVal == 6) { tt = "其他"; }
            else { tt = "未知"; }
            List<PropRepairItem> items = repairItemMapper.selectList(new LambdaQueryWrapper<PropRepairItem>().eq(PropRepairItem::getRepairId, r.getId()));
            BigDecimal tc = items.stream().map(i -> i.getTotalCost() != null ? i.getTotalCost() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
            return new Response.RepairInfo(r.getRepairNo(), r.getTitle(), tt, st, r.getHandlerName(), r.getEvaluateScore(), tc, r.getCreateTime() != null ? r.getCreateTime().toString() : "");
        }).toList();

        long pending = repairs.stream().filter(r -> r.getStatus() <= 1).count();
        long completed = repairs.stream().filter(r -> r.getStatus() == 3).count();
        return new Response(repairs.size(), pending, completed, list);
    }
}
