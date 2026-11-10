package com.smartcommunity.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.smartcommunity.entity.SocialPhone;
import com.smartcommunity.mapper.SocialPhoneMapper;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

@Component
@Description("查询便民电话：物业、医院、维修、快递等常用电话")
public class PhoneQueryTool implements Function<PhoneQueryTool.Request, PhoneQueryTool.Response> {

    private final SocialPhoneMapper mapper;
    public PhoneQueryTool(SocialPhoneMapper mapper) { this.mapper = mapper; }

    public record Request(@JsonPropertyDescription("分类：物业/医疗/教育/生活服务/政府/交通/餐饮/ALL") String category) {}
    public record Response(int count, List<PhoneInfo> phones) { public record PhoneInfo(String category, String name, String phone, String workTime, String address) {} }

    @Override
    public Response apply(Request req) {
        LambdaQueryWrapper<SocialPhone> qw = new LambdaQueryWrapper<>();
        qw.eq(SocialPhone::getStatus, 1);
        String cat = req.category();
        if (cat != null && !"ALL".equals(cat)) qw.eq(SocialPhone::getCategory, cat);
        qw.orderByDesc(SocialPhone::getIsRecommend).orderByAsc(SocialPhone::getSort);
        List<SocialPhone> list = mapper.selectList(qw);
        List<Response.PhoneInfo> pi = list.stream().map(p -> new Response.PhoneInfo(p.getCategory(), p.getName(), p.getPhone(), p.getWorkTime(), p.getAddress())).toList();
        return new Response(pi.size(), pi);
    }
}
