package com.smartcommunity.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.smartcommunity.entity.SocialVote;
import com.smartcommunity.entity.SocialVoteOption;
import com.smartcommunity.mapper.SocialVoteMapper;
import com.smartcommunity.mapper.SocialVoteOptionMapper;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

@Component
@Description("查询社区投票信息：进行中或已结束的投票及其选项")
public class VoteQueryTool implements Function<VoteQueryTool.Request, VoteQueryTool.Response> {

    private final SocialVoteMapper voteMapper;
    private final SocialVoteOptionMapper optionMapper;

    public VoteQueryTool(SocialVoteMapper voteMapper, SocialVoteOptionMapper optionMapper) {
        this.voteMapper = voteMapper;
        this.optionMapper = optionMapper;
    }

    public record Request(
            @JsonPropertyDescription("状态：ACTIVE=进行中, ENDED=已结束, ALL=全部")
            String status) {}

    public record Response(int count, List<VoteInfo> votes) {
        public record VoteInfo(String title, String description, String status, int totalVotes, List<OptionInfo> options) {}
        public record OptionInfo(String text, int voteCount) {}
    }

    @Override
    public Response apply(Request req) {
        LambdaQueryWrapper<SocialVote> qw = new LambdaQueryWrapper<>();
        String st = req.status();
        if ("ACTIVE".equals(st)) qw.eq(SocialVote::getStatus, 1);
        else if ("ENDED".equals(st)) qw.eq(SocialVote::getStatus, 2);
        qw.orderByDesc(SocialVote::getCreateTime);
        List<SocialVote> votes = voteMapper.selectList(qw);
        List<Response.VoteInfo> vi = votes.stream().map(v -> {
            int voteStatusVal = v.getStatus() != null ? v.getStatus() : 0;
            String sv;
            if (voteStatusVal == 0) { sv = "未开始"; }
            else if (voteStatusVal == 1) { sv = "进行中"; }
            else if (voteStatusVal == 2) { sv = "已结束"; }
            else { sv = "未知"; }
            List<SocialVoteOption> opts = optionMapper.selectList(new LambdaQueryWrapper<SocialVoteOption>().eq(SocialVoteOption::getVoteId, v.getId()).orderByDesc(SocialVoteOption::getVoteCount));
            List<Response.OptionInfo> oi = opts.stream().map(o -> new Response.OptionInfo(o.getOptionText(), o.getVoteCount() != null ? o.getVoteCount() : 0)).toList();
            return new Response.VoteInfo(v.getTitle(), v.getDescription(), sv, v.getTotalVotes() != null ? v.getTotalVotes() : 0, oi);
        }).toList();
        return new Response(vi.size(), vi);
    }
}
