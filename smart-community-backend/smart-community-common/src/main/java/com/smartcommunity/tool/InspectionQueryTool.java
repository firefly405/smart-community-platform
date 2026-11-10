package com.smartcommunity.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.smartcommunity.entity.PropCleaning;
import com.smartcommunity.entity.PropInspection;
import com.smartcommunity.mapper.PropCleaningMapper;
import com.smartcommunity.mapper.PropInspectionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
@Description("查询巡检记录和保洁记录：巡检异常、保洁质量评分、区域状态")
public class InspectionQueryTool implements Function<InspectionQueryTool.Request, InspectionQueryTool.Response> {

    private final PropInspectionMapper inspectionMapper;
    private final PropCleaningMapper cleaningMapper;

    public record Request(
            @JsonPropertyDescription("区域名称，如A栋、B栋、公共区域等")
            String area,
            @JsonPropertyDescription("返回条数，默认10")
            Integer limit) {}

    public record Response(int totalInspections, long anomalyCount, List<InspectionInfo> inspections,
                           int totalCleanings, List<CleaningInfo> cleanings) {
        public record InspectionInfo(String area, String inspector, String status, String date, String anomalies) {}
        public record CleaningInfo(String area, String cleaner, String status, String date, Integer qualityScore) {}
    }

    @Override
    public Response apply(Request req) {
        int l = (req.limit() != null && req.limit() > 0) ? req.limit() : 10;
        String area = req.area();

        LambdaQueryWrapper<PropInspection> iqw = new LambdaQueryWrapper<>();
        if (area != null) iqw.eq(PropInspection::getArea, area);
        iqw.orderByDesc(PropInspection::getInspectionDate).last("LIMIT " + l);
        List<PropInspection> inspections = inspectionMapper.selectList(iqw);
        List<Response.InspectionInfo> il = inspections.stream().map(i -> {
            int inspectionStatusVal = i.getStatus() != null ? i.getStatus() : 0;
            String st;
            if (inspectionStatusVal == 0) { st = "未开始"; }
            else if (inspectionStatusVal == 1) { st = "进行中"; }
            else if (inspectionStatusVal == 2) { st = "已完成"; }
            else { st = "未知"; }
            return new Response.InspectionInfo(i.getArea(), i.getInspectorName(), st, i.getInspectionDate() != null ? i.getInspectionDate().toString() : "", i.getAnomalies());
        }).toList();
        long anomalies = inspections.stream().filter(i -> i.getAnomalies() != null && !i.getAnomalies().isEmpty()).count();

        LambdaQueryWrapper<PropCleaning> cqw = new LambdaQueryWrapper<>();
        if (area != null) cqw.eq(PropCleaning::getArea, area);
        cqw.orderByDesc(PropCleaning::getTaskDate).last("LIMIT " + l);
        List<PropCleaning> cleanings = cleaningMapper.selectList(cqw);
        List<Response.CleaningInfo> cl = cleanings.stream().map(c -> {
            int cleaningStatusVal = c.getStatus() != null ? c.getStatus() : 0;
            String st;
            if (cleaningStatusVal == 0) { st = "待处理"; }
            else if (cleaningStatusVal == 1) { st = "处理中"; }
            else if (cleaningStatusVal == 2) { st = "已完成"; }
            else { st = "未知"; }
            return new Response.CleaningInfo(c.getArea(), c.getCleanerName(), st, c.getTaskDate() != null ? c.getTaskDate().toString() : "", c.getQualityScore());
        }).toList();
        return new Response(il.size(), anomalies, il, cl.size(), cl);
    }
}
