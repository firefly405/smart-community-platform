package com.smartcommunity.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.smartcommunity.entity.GuardVisitor;
import com.smartcommunity.mapper.GuardVisitorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
@Description("查询业主的访客记录：来访历史、当前在访、待审核访客")
public class VisitorQueryTool implements Function<VisitorQueryTool.Request, VisitorQueryTool.Response> {

    private final GuardVisitorMapper visitorMapper;

    public record Request(
            @JsonProperty(required = true)
            @JsonPropertyDescription("业主ID")
            Long ownerId,
            @JsonPropertyDescription("状态筛选: PENDING=待审核, APPROVED=已通过, CHECKED_IN=已签到, COMPLETED=已签退, ALL=全部")
            String statusFilter) {}

    public record Response(int totalCount, long currentCount, List<VisitorInfo> visitors) {
        public record VisitorInfo(String name, String phone, String reason, String status,
                                   String startTime, String endTime, String plateNo) {}
    }

    @Override
    public Response apply(Request req) {
        LambdaQueryWrapper<GuardVisitor> qw = new LambdaQueryWrapper<>();
        qw.eq(GuardVisitor::getOwnerId, req.ownerId());
        String sf = req.statusFilter();
        if (sf != null && !"ALL".equals(sf)) {
            int sc;
            if ("PENDING".equals(sf)) { sc = 0; }
            else if ("APPROVED".equals(sf)) { sc = 1; }
            else if ("CHECKED_IN".equals(sf)) { sc = 3; }
            else if ("COMPLETED".equals(sf)) { sc = 4; }
            else { sc = -1; }
            if (sc >= 0) qw.eq(GuardVisitor::getStatus, sc);
        }
        qw.orderByDesc(GuardVisitor::getVisitStartTime);
        List<GuardVisitor> list = visitorMapper.selectList(qw);
        List<Response.VisitorInfo> vi = list.stream().map(v -> {
            int visitorStatusVal = v.getStatus() != null ? v.getStatus() : 0;
            String st;
            if (visitorStatusVal == 0) { st = "待审核"; }
            else if (visitorStatusVal == 1) { st = "已通过"; }
            else if (visitorStatusVal == 2) { st = "已拒绝"; }
            else if (visitorStatusVal == 3) { st = "已签到"; }
            else if (visitorStatusVal == 4) { st = "已签退"; }
            else if (visitorStatusVal == 5) { st = "已过期"; }
            else { st = "未知"; }
            return new Response.VisitorInfo(v.getVisitorName(), v.getVisitorPhone(), v.getVisitReason(), st, v.getVisitStartTime() != null ? v.getVisitStartTime().toString() : "", v.getVisitEndTime() != null ? v.getVisitEndTime().toString() : "", v.getPlateNo());
        }).toList();
        long current = list.stream().filter(v -> v.getStatus() != null && v.getStatus() == 3).count();
        return new Response(list.size(), current, vi);
    }
}
