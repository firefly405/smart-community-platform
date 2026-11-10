package com.smartcommunity.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.smartcommunity.entity.LifeProduct;
import com.smartcommunity.mapper.LifeProductMapper;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

@Component
@Description("查询社区商品信息：在售商品、价格、库存、销量")
public class ProductQueryTool implements Function<ProductQueryTool.Request, ProductQueryTool.Response> {

    private final LifeProductMapper mapper;
    public ProductQueryTool(LifeProductMapper mapper) { this.mapper = mapper; }

    public record Request(@JsonPropertyDescription("返回条数，默认10") Integer limit) {}
    public record Response(int count, List<ProductInfo> products) { public record ProductInfo(String name, String price, String originPrice, int stock, int sales) {} }

    @Override
    public Response apply(Request req) {
        int l = (req.limit() != null && req.limit() > 0) ? req.limit() : 10;
        List<LifeProduct> list = mapper.selectList(new LambdaQueryWrapper<LifeProduct>().eq(LifeProduct::getIsOnSale, 1).orderByDesc(LifeProduct::getIsRecommend).orderByDesc(LifeProduct::getSales).last("LIMIT " + l));
        List<Response.ProductInfo> pi = list.stream().map(p -> new Response.ProductInfo(p.getProductName(), p.getPrice() != null ? p.getPrice().toString() : "", p.getOriginPrice() != null ? p.getOriginPrice().toString() : "", p.getStock() != null ? p.getStock() : 0, p.getSales() != null ? p.getSales() : 0)).toList();
        return new Response(pi.size(), pi);
    }
}
