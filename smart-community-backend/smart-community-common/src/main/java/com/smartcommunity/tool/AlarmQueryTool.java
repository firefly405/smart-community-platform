package com.smartcommunity.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.smartcommunity.entity.GuardAlarm;
import com.smartcommunity.mapper.GuardAlarmMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
@Description("查询小区报警记录：消防报警、燃气报警、入侵报警、故障报警等")
public class AlarmQueryTool implements Function<AlarmQueryTool.Request, AlarmQueryTool.Response> {

    private final GuardAlarmMapper alarmMapper;

    public record Request(
            @JsonPropertyDescription("报警类型：FIRE=消防, GAS=燃气, INTRUSION=入侵, FAULT=故障, EMERGENCY=紧急, ALL=全部")
            String alarmType,
            @JsonPropertyDescription("状态：PENDING=待处理, PROCESSING=处理中, RESOLVED=已处理, FALSE_ALARM=误报, ALL=全部")
            String statusFilter,
            @JsonPropertyDescription("返回条数，默认10")
            Integer limit) {}

    public record Response(int totalCount, long unresolvedCount, List<AlarmInfo> alarms) {
        public record AlarmInfo(String type, String level, String status, String location,
                                String description, String alarmTime, String handlerName) {}
    }

    @Override
    public Response apply(Request req) {
        int l = (req.limit() != null && req.limit() > 0) ? req.limit() : 10;
        LambdaQueryWrapper<GuardAlarm> qw = new LambdaQueryWrapper<>();
        String at = req.alarmType();
        String sf = req.statusFilter();
        if (at != null && !"ALL".equals(at)) {
            int t;
            if ("FIRE".equals(at)) { t = 1; }
            else if ("GAS".equals(at)) { t = 2; }
            else if ("INTRUSION".equals(at)) { t = 3; }
            else if ("FAULT".equals(at)) { t = 4; }
            else if ("EMERGENCY".equals(at)) { t = 5; }
            else { t = -1; }
            if (t >= 0) qw.eq(GuardAlarm::getAlarmType, t);
        }
        if (sf != null && !"ALL".equals(sf)) {
            int s;
            if ("PENDING".equals(sf)) { s = 0; }
            else if ("PROCESSING".equals(sf)) { s = 1; }
            else if ("RESOLVED".equals(sf)) { s = 2; }
            else if ("FALSE_ALARM".equals(sf)) { s = 3; }
            else { s = -1; }
            if (s >= 0) qw.eq(GuardAlarm::getStatus, s);
        }
        qw.orderByDesc(GuardAlarm::getAlarmTime).last("LIMIT " + l);
        List<GuardAlarm> alarms = alarmMapper.selectList(qw);
        List<Response.AlarmInfo> ai = alarms.stream().map(a -> {
            int alarmTypeVal = a.getAlarmType() != null ? a.getAlarmType() : 1;
            String tt;
            if (alarmTypeVal == 1) { tt = "消防"; }
            else if (alarmTypeVal == 2) { tt = "燃气"; }
            else if (alarmTypeVal == 3) { tt = "入侵"; }
            else if (alarmTypeVal == 4) { tt = "故障"; }
            else if (alarmTypeVal == 5) { tt = "紧急"; }
            else if (alarmTypeVal == 6) { tt = "其他"; }
            else { tt = "其他"; }
            int alarmStatusVal = a.getStatus() != null ? a.getStatus() : 0;
            String st;
            if (alarmStatusVal == 0) { st = "待处理"; }
            else if (alarmStatusVal == 1) { st = "处理中"; }
            else if (alarmStatusVal == 2) { st = "已处理"; }
            else if (alarmStatusVal == 3) { st = "误报"; }
            else { st = "未知"; }
            int alarmLevelVal = a.getAlarmLevel() != null ? a.getAlarmLevel() : 1;
            String lv;
            if (alarmLevelVal == 1) { lv = "一般"; }
            else if (alarmLevelVal == 2) { lv = "重要"; }
            else if (alarmLevelVal == 3) { lv = "紧急"; }
            else { lv = "一般"; }
            return new Response.AlarmInfo(tt, lv, st, a.getLocation(), a.getDescription(), a.getAlarmTime() != null ? a.getAlarmTime().toString() : "", a.getHandlerName());
        }).toList();
        long unresolved = alarms.stream().filter(a -> a.getStatus() != null && a.getStatus() <= 1).count();
        return new Response(alarms.size(), unresolved, ai);
    }
}
