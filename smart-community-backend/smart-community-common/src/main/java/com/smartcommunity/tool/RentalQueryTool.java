package com.smartcommunity.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.smartcommunity.entity.LifeRental;
import com.smartcommunity.mapper.LifeRentalMapper;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

@Component
@Description("查询房屋租赁信息：在租房源、月租、户型、联系方式")
public class RentalQueryTool implements Function<RentalQueryTool.Request, RentalQueryTool.Response> {

    private final LifeRentalMapper mapper;
    public RentalQueryTool(LifeRentalMapper mapper) { this.mapper = mapper; }

    public record Request(@JsonPropertyDescription("返回条数，默认10") Integer limit) {}
    public record Response(int count, List<RentalInfo> rentals) { public record RentalInfo(String title, String type, String monthlyRent, String contactPhone, String description) {} }

    @Override
    public Response apply(Request req) {
        int l = (req.limit() != null && req.limit() > 0) ? req.limit() : 10;
        List<LifeRental> list = mapper.selectList(new LambdaQueryWrapper<LifeRental>().eq(LifeRental::getStatus, 1).orderByDesc(LifeRental::getIsRecommend).orderByDesc(LifeRental::getPublishTime).last("LIMIT " + l));
        List<Response.RentalInfo> ri = list.stream().map(r -> {
            int rentalTypeVal = r.getRentalType() != null ? r.getRentalType() : 1;
            String tt;
            if (rentalTypeVal == 1) { tt = "整租"; }
            else if (rentalTypeVal == 2) { tt = "合租"; }
            else if (rentalTypeVal == 3) { tt = "短租"; }
            else { tt = "其他"; }
            return new Response.RentalInfo(r.getTitle(), tt, r.getMonthlyRent() != null ? r.getMonthlyRent().toString() : "", r.getContactPhone(), r.getDescription());
        }).toList();
        return new Response(ri.size(), ri);
    }
}
