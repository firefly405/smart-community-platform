package com.smartcommunity.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.smartcommunity.entity.SocialPost;
import com.smartcommunity.mapper.SocialPostMapper;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

@Component
@Description("查询社区帖子：最新帖子、热门帖子、按分类筛选")
public class PostQueryTool implements Function<PostQueryTool.Request, PostQueryTool.Response> {

    private final SocialPostMapper postMapper;

    public PostQueryTool(SocialPostMapper postMapper) { this.postMapper = postMapper; }

    public record Request(
            @JsonPropertyDescription("分类：HELP=求助, SHARE=分享, TRADE=交易, ACTIVITY=活动, COMPLAINT=吐槽, ALL=全部")
            String category,
            @JsonPropertyDescription("返回条数，默认10")
            Integer limit) {}

    public record Response(int count, List<PostInfo> posts) {
        public record PostInfo(String title, String summary, String category, int viewCount, int likeCount, int commentCount) {}
    }

    @Override
    public Response apply(Request req) {
        int l = (req.limit() != null && req.limit() > 0) ? req.limit() : 10;
        LambdaQueryWrapper<SocialPost> qw = new LambdaQueryWrapper<>();
        qw.eq(SocialPost::getStatus, 1);
        String cat = req.category();
        if (cat != null && !"ALL".equals(cat)) {
            int c;
            if ("HELP".equals(cat)) { c = 1; }
            else if ("SHARE".equals(cat)) { c = 2; }
            else if ("TRADE".equals(cat)) { c = 3; }
            else if ("ACTIVITY".equals(cat)) { c = 4; }
            else if ("COMPLAINT".equals(cat)) { c = 5; }
            else { c = -1; }
            if (c >= 0) qw.eq(SocialPost::getCategory, c);
        }
        qw.orderByDesc(SocialPost::getIsTop).orderByDesc(SocialPost::getCreateTime).last("LIMIT " + l);
        List<SocialPost> posts = postMapper.selectList(qw);
        List<Response.PostInfo> pi = posts.stream().map(p -> {
            String catStr = p.getCategory();
            int postCategoryVal;
            try { postCategoryVal = catStr != null ? Integer.parseInt(catStr) : 1; }
            catch (NumberFormatException e) { postCategoryVal = 1; }
            String ct;
            if (postCategoryVal == 1) { ct = "求助"; }
            else if (postCategoryVal == 2) { ct = "分享"; }
            else if (postCategoryVal == 3) { ct = "交易"; }
            else if (postCategoryVal == 4) { ct = "活动"; }
            else if (postCategoryVal == 5) { ct = "吐槽"; }
            else if (postCategoryVal == 6) { ct = "其他"; }
            else { ct = "其他"; }
            String summary = p.getContent() != null && p.getContent().length() > 150 ? p.getContent().substring(0, 150) + "..." : p.getContent();
            return new Response.PostInfo(p.getTitle(), summary, ct, p.getViewCount() != null ? p.getViewCount() : 0, p.getLikeCount() != null ? p.getLikeCount() : 0, p.getCommentCount() != null ? p.getCommentCount() : 0);
        }).toList();
        return new Response(pi.size(), pi);
    }
}
