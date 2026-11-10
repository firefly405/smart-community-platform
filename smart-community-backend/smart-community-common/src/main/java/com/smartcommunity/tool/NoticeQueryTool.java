package com.smartcommunity.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.smartcommunity.entity.PropNotice;
import com.smartcommunity.mapper.PropNoticeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
@Description("查询物业通知公告：最新发布、置顶公告、紧急通知、分类筛选")
public class NoticeQueryTool implements Function<NoticeQueryTool.Request, NoticeQueryTool.Response> {

    private final PropNoticeMapper noticeMapper;

    public record Request(
            @JsonPropertyDescription("返回条数，默认10")
            Integer limit,
            @JsonPropertyDescription("分类：NOTICE=通知, ACTIVITY=活动, EMERGENCY=紧急, ALL=全部")
            String type,
            @JsonPropertyDescription("是否只查置顶")
            Boolean topOnly) {}

    public record Response(int count, List<NoticeInfo> notices) {
        public record NoticeInfo(String title, String summary, String type, boolean isTop, String publishTime, int viewCount) {}
    }

    @Override
    public Response apply(Request req) {
        int l = (req.limit() != null && req.limit() > 0) ? req.limit() : 10;
        String type = req.type();
        Boolean topOnly = req.topOnly();

        LambdaQueryWrapper<PropNotice> qw = new LambdaQueryWrapper<>();
        qw.eq(PropNotice::getIsPublish, 1);
        if (type != null && !"ALL".equals(type)) {
            int tt;
            if ("NOTICE".equals(type)) { tt = 1; }
            else if ("ACTIVITY".equals(type)) { tt = 2; }
            else if ("EMERGENCY".equals(type)) { tt = 3; }
            else { tt = -1; }
            if (tt >= 0) qw.eq(PropNotice::getNoticeType, tt);
        }
        if (Boolean.TRUE.equals(topOnly)) qw.eq(PropNotice::getIsTop, 1);
        qw.orderByDesc(PropNotice::getIsTop).orderByDesc(PropNotice::getPublishTime).last("LIMIT " + l);

        List<PropNotice> notices = noticeMapper.selectList(qw);
        List<Response.NoticeInfo> list = notices.stream().map(n -> {
            int noticeTypeVal = n.getNoticeType() != null ? n.getNoticeType() : 1;
            String typeText;
            if (noticeTypeVal == 1) { typeText = "通知"; }
            else if (noticeTypeVal == 2) { typeText = "活动"; }
            else if (noticeTypeVal == 3) { typeText = "紧急"; }
            else { typeText = "其他"; }
            String summary = n.getContent() != null && n.getContent().length() > 200 ? n.getContent().substring(0, 200) + "..." : n.getContent();
            return new Response.NoticeInfo(n.getTitle(), summary, typeText, n.getIsTop() != null && n.getIsTop() == 1, n.getPublishTime() != null ? n.getPublishTime().toString() : "", n.getViewCount() != null ? n.getViewCount() : 0);
        }).toList();

        return new Response(list.size(), list);
    }
}
