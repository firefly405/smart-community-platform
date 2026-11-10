package com.smartcommunity.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.smartcommunity.entity.*;
import com.smartcommunity.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
@Description("查询社区活动：正在进行、即将开始、已结束的活动，以及报名情况")
public class ActivityQueryTool implements Function<ActivityQueryTool.Request, ActivityQueryTool.Response> {

    private final LifeActivityMapper activityMapper;
    private final LifeActivityJoinMapper joinMapper;

    public record Request(
            @JsonPropertyDescription("查询范围: IN_PROGRESS=进行中, UPCOMING=即将开始, PAST=已结束, ALL=全部")
            String scope,
            @JsonPropertyDescription("返回条数，默认10")
            Integer limit) {}

    public record Response(int count, List<ActivityInfo> activities) {
        public record ActivityInfo(String title, String type, String status, String location,
                                   String startTime, String endTime, int currentParticipants,
                                   int maxParticipants, String contactPhone) {}
    }

    @Override
    public Response apply(Request req) {
        int l = (req.limit() != null && req.limit() > 0) ? req.limit() : 10;
        String scope = req.scope();

        LambdaQueryWrapper<LifeActivity> qw = new LambdaQueryWrapper<>();
        if ("IN_PROGRESS".equals(scope)) qw.eq(LifeActivity::getStatus, 2);
        else if ("UPCOMING".equals(scope)) qw.in(LifeActivity::getStatus, 0, 1);
        else if ("PAST".equals(scope)) qw.eq(LifeActivity::getStatus, 3);
        qw.orderByDesc(LifeActivity::getIsTop).orderByDesc(LifeActivity::getCreateTime).last("LIMIT " + l);

        List<LifeActivity> activities = activityMapper.selectList(qw);
        List<Response.ActivityInfo> list = activities.stream().map(a -> {
            int activityStatusVal = a.getStatus() != null ? a.getStatus() : 0;
            String st;
            if (activityStatusVal == 0) { st = "草稿"; }
            else if (activityStatusVal == 1) { st = "已发布"; }
            else if (activityStatusVal == 2) { st = "进行中"; }
            else if (activityStatusVal == 3) { st = "已结束"; }
            else if (activityStatusVal == 4) { st = "已取消"; }
            else { st = "未知"; }
            int activityTypeVal = a.getActivityType() != null ? a.getActivityType() : 1;
            String tt;
            if (activityTypeVal == 1) { tt = "文体"; }
            else if (activityTypeVal == 2) { tt = "公益"; }
            else if (activityTypeVal == 3) { tt = "节日"; }
            else if (activityTypeVal == 4) { tt = "讲座"; }
            else if (activityTypeVal == 5) { tt = "其他"; }
            else { tt = "其他"; }
            return new Response.ActivityInfo(a.getTitle(), tt, st, a.getLocation(),
                    a.getStartTime() != null ? a.getStartTime().toString() : "",
                    a.getEndTime() != null ? a.getEndTime().toString() : "",
                    a.getCurrentParticipants() != null ? a.getCurrentParticipants() : 0,
                    a.getMaxParticipants() != null ? a.getMaxParticipants() : 0,
                    a.getContactPhone());
        }).toList();
        return new Response(list.size(), list);
    }
}
