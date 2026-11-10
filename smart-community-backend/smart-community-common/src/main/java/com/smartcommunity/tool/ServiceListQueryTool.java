package com.smartcommunity.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.smartcommunity.entity.LifeService;
import com.smartcommunity.mapper.LifeServiceMapper;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

@Component
@Description("查询家政便民服务项目列表和价格")
public class ServiceListQueryTool implements Function<ServiceListQueryTool.Request, ServiceListQueryTool.Response> {

    private final LifeServiceMapper mapper;
    public ServiceListQueryTool(LifeServiceMapper mapper) { this.mapper = mapper; }

    public record Request(@JsonPropertyDescription("服务类型：CLEANING=保洁, BABYSITTING=保姆月嫂, REPAIR=维修, MOVING=搬家, ALL=全部") String serviceType) {}
    public record Response(int count, List<ServiceInfo> services) { public record ServiceInfo(String name, String price, String unit, String description) {} }

    @Override
    public Response apply(Request req) {
        LambdaQueryWrapper<LifeService> qw = new LambdaQueryWrapper<>();
        qw.eq(LifeService::getIsOnSale, 1);
        String st = req.serviceType();
        if (st != null && !"ALL".equals(st)) {
            int sc;
            if ("CLEANING".equals(st)) { sc = 1; }
            else if ("BABYSITTING".equals(st)) { sc = 2; }
            else if ("REPAIR".equals(st)) { sc = 4; }
            else if ("MOVING".equals(st)) { sc = 5; }
            else { sc = -1; }
            if (sc >= 0) qw.eq(LifeService::getServiceType, sc);
        }
        List<LifeService> list = mapper.selectList(qw);
        List<Response.ServiceInfo> si = list.stream().map(s -> new Response.ServiceInfo(s.getServiceName(), s.getPrice() != null ? s.getPrice().toString() : "", s.getUnit() != null ? s.getUnit() : "次", s.getDescription())).toList();
        return new Response(si.size(), si);
    }
}
