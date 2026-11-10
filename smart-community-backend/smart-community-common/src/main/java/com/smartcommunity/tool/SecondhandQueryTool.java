package com.smartcommunity.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.smartcommunity.entity.SocialSecondhand;
import com.smartcommunity.mapper.SocialSecondhandMapper;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

@Component
@Description("查询社区二手交易物品：在售物品、价格、成色")
public class SecondhandQueryTool implements Function<SecondhandQueryTool.Request, SecondhandQueryTool.Response> {

    private final SocialSecondhandMapper secondhandMapper;

    public SecondhandQueryTool(SocialSecondhandMapper secondhandMapper) { this.secondhandMapper = secondhandMapper; }

    public record Request(
            @JsonPropertyDescription("分类：FURNITURE=家具, APPLIANCE=家电, DIGITAL=数码, BABY=母婴, BOOK=图书, ALL=全部")
            String category,
            @JsonPropertyDescription("返回条数，默认10")
            Integer limit) {}

    public record Response(int count, List<ItemInfo> items) {
        public record ItemInfo(String title, String price, String originPrice, String condition, String contactPhone) {}
    }

    @Override
    public Response apply(Request req) {
        int l = (req.limit() != null && req.limit() > 0) ? req.limit() : 10;
        LambdaQueryWrapper<SocialSecondhand> qw = new LambdaQueryWrapper<>();
        qw.eq(SocialSecondhand::getStatus, 0);
        String cat = req.category();
        if (cat != null && !"ALL".equals(cat)) {
            int c;
            if ("FURNITURE".equals(cat)) { c = 1; }
            else if ("APPLIANCE".equals(cat)) { c = 2; }
            else if ("DIGITAL".equals(cat)) { c = 3; }
            else if ("BABY".equals(cat)) { c = 4; }
            else if ("BOOK".equals(cat)) { c = 5; }
            else { c = -1; }
            if (c >= 0) qw.eq(SocialSecondhand::getCategory, c);
        }
        qw.orderByDesc(SocialSecondhand::getCreateTime).last("LIMIT " + l);
        List<SocialSecondhand> items = secondhandMapper.selectList(qw);
        List<Response.ItemInfo> ii = items.stream().map(i -> {
            int conditionLevelVal = i.getConditionLevel() != null ? i.getConditionLevel() : 1;
            String cond;
            if (conditionLevelVal == 1) { cond = "全新"; }
            else if (conditionLevelVal == 2) { cond = "九成新"; }
            else if (conditionLevelVal == 3) { cond = "八成新"; }
            else if (conditionLevelVal == 4) { cond = "五成新"; }
            else { cond = "其他"; }
            return new Response.ItemInfo(i.getTitle(), i.getPrice() != null ? i.getPrice().toString() : "", i.getOriginPrice() != null ? i.getOriginPrice().toString() : "", cond, i.getContactPhone());
        }).toList();
        return new Response(ii.size(), ii);
    }
}
