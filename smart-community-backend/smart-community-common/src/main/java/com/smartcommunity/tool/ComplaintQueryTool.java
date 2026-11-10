package com.smartcommunity.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.smartcommunity.entity.PropComplaint;
import com.smartcommunity.mapper.PropComplaintMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
@Description("查询业主投诉建议的状态和处理结果。可查全部或指定状态")
public class ComplaintQueryTool implements Function<ComplaintQueryTool.Request, ComplaintQueryTool.Response> {

    private final PropComplaintMapper complaintMapper;

    public record Request(
            @JsonProperty(required = true)
            @JsonPropertyDescription("业主ID")
            Long ownerId,
            @JsonPropertyDescription("状态筛选: PENDING=待处理, PROCESSING=处理中, COMPLETED=已完成, CLOSED=已关闭, ALL=全部")
            String statusFilter) {}

    public record Response(int totalCount, long unresolvedCount, List<ComplaintInfo> complaints) {
        public record ComplaintInfo(String complaintNo, String title, String type, String status,
                                     String handlerName, String replyContent,
                                     Integer evaluateScore, String createTime) {}
    }

    @Override
    public Response apply(Request req) {
        Long ownerId = req.ownerId();
        String sf = req.statusFilter();

        LambdaQueryWrapper<PropComplaint> qw = new LambdaQueryWrapper<>();
        qw.eq(PropComplaint::getOwnerId, ownerId);
        if (sf != null && !"ALL".equals(sf)) {
            int sc;
            if ("PENDING".equals(sf)) { sc = 0; }
            else if ("PROCESSING".equals(sf)) { sc = 1; }
            else if ("COMPLETED".equals(sf)) { sc = 2; }
            else if ("CLOSED".equals(sf)) { sc = 3; }
            else { sc = -1; }
            if (sc >= 0) qw.eq(PropComplaint::getStatus, sc);
        }
        qw.orderByDesc(PropComplaint::getId);

        List<PropComplaint> complaints = complaintMapper.selectList(qw);
        List<Response.ComplaintInfo> list = complaints.stream().map(c -> {
            Integer complaintStatus = c.getStatus();
            String st;
            if (complaintStatus == null) { st = "未知"; }
            else if (complaintStatus == 0) { st = "待处理"; }
            else if (complaintStatus == 1) { st = "处理中"; }
            else if (complaintStatus == 2) { st = "已完成"; }
            else if (complaintStatus == 3) { st = "已关闭"; }
            else { st = "未知"; }
            int complaintTypeVal = c.getComplaintType() != null ? c.getComplaintType() : 1;
            String tt;
            if (complaintTypeVal == 1) { tt = "服务态度"; }
            else if (complaintTypeVal == 2) { tt = "环境卫生"; }
            else if (complaintTypeVal == 3) { tt = "设施设备"; }
            else if (complaintTypeVal == 4) { tt = "其他"; }
            else { tt = "未知"; }
            return new Response.ComplaintInfo(c.getComplaintNo(), c.getTitle(), tt, st, c.getHandlerName(), c.getReplyContent(), c.getEvaluateScore(), c.getCreateTime() != null ? c.getCreateTime().toString() : "");
        }).toList();

        long unresolved = complaints.stream().filter(c -> c.getStatus() <= 1).count();
        return new Response(complaints.size(), unresolved, list);
    }
}
