package com.smartcommunity.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.smartcommunity.entity.GuardAccessLog;
import com.smartcommunity.mapper.GuardAccessLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
@Description("查询门禁通行记录：通行时间、进出方向、通行方式、成功/失败")
public class AccessLogQueryTool implements Function<AccessLogQueryTool.Request, AccessLogQueryTool.Response> {

    private final GuardAccessLogMapper accessLogMapper;

    public record Request(
            @JsonProperty(required = true)
            @JsonPropertyDescription("用户ID")
            Long userId,
            @JsonPropertyDescription("返回条数，默认20")
            Integer limit) {}

    public record Response(int totalCount, long failedCount, List<LogEntry> logs) {
        public record LogEntry(String accessTime, String accessType, String direction, String result, String failReason) {}
    }

    @Override
    public Response apply(Request req) {
        int l = (req.limit() != null && req.limit() > 0) ? req.limit() : 20;
        List<GuardAccessLog> logs = accessLogMapper.selectList(
                new LambdaQueryWrapper<GuardAccessLog>().eq(GuardAccessLog::getUserId, req.userId())
                        .orderByDesc(GuardAccessLog::getAccessTime).last("LIMIT " + l));
        List<Response.LogEntry> le = logs.stream().map(log -> {
            int accessTypeVal = log.getAccessType() != null ? log.getAccessType() : 1;
            String tt;
            if (accessTypeVal == 1) { tt = "人脸"; }
            else if (accessTypeVal == 2) { tt = "刷卡"; }
            else if (accessTypeVal == 3) { tt = "密码"; }
            else if (accessTypeVal == 4) { tt = "二维码"; }
            else if (accessTypeVal == 5) { tt = "远程开门"; }
            else { tt = "其他"; }
            String dir = (log.getAccessDirection() != null && log.getAccessDirection() == 1) ? "进" : "出";
            String res = (log.getAccessResult() != null && log.getAccessResult() == 1) ? "成功" : "失败";
            return new Response.LogEntry(log.getAccessTime() != null ? log.getAccessTime().toString() : "", tt, dir, res, log.getFailReason());
        }).toList();
        long fc = logs.stream().filter(log -> log.getAccessResult() != null && log.getAccessResult() == 2).count();
        return new Response(logs.size(), fc, le);
    }
}
