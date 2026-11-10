package com.smartcommunity.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.smartcommunity.entity.BaseParking;
import com.smartcommunity.entity.BaseVehicle;
import com.smartcommunity.mapper.BaseParkingMapper;
import com.smartcommunity.mapper.BaseVehicleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
@Description("查询小区车位信息：空闲车位数量、位置分布、地上/地下类型。可查全局统计或指定业主的车位")
public class ParkingQueryTool implements Function<ParkingQueryTool.Request, ParkingQueryTool.Response> {

    private final BaseParkingMapper parkingMapper;
    private final BaseVehicleMapper vehicleMapper;

    public record Request(
            @JsonPropertyDescription("查询类型: ALL_STATS=全局统计, MY_SPOTS=我的车位")
            String queryType,
            @JsonPropertyDescription("业主ID，查'我的车位'时需要")
            Long ownerId) {}

    public record Response(long freeCount, long soldCount, long rentedCount,
                           List<SpotInfo> freeSpots, List<SpotInfo> mySpots) {
        public record SpotInfo(String parkingNo, String area, String type, String status) {}
    }

    @Override
    public Response apply(Request req) {
        String qt = req.queryType();
        Long ownerId = req.ownerId();

        List<BaseParking> allParking = parkingMapper.selectList(new LambdaQueryWrapper<>());
        long freeCount = allParking.stream().filter(p -> p.getStatus() == 0).count();
        long soldCount = allParking.stream().filter(p -> p.getStatus() == 1).count();
        long rentedCount = allParking.stream().filter(p -> p.getStatus() == 2).count();

        List<Response.SpotInfo> freeSpots = allParking.stream()
                .filter(p -> p.getStatus() == 0)
                .map(p -> new Response.SpotInfo(
                        p.getParkingNo(), p.getArea(),
                        p.getParkingType() == 1 ? "地上" : "地下", "空闲"))
                .toList();

        List<Response.SpotInfo> mySpots = List.of();
        if (ownerId != null) {
            mySpots = vehicleMapper.selectList(
                    new LambdaQueryWrapper<BaseVehicle>().eq(BaseVehicle::getOwnerId, ownerId))
                    .stream()
                    .filter(v -> v.getParkingId() != null)
                    .map(v -> {
                        BaseParking p = parkingMapper.selectById(v.getParkingId());
                        String statusText = p != null ? (p.getStatus() == 1 ? "已售" : p.getStatus() == 2 ? "已租" : "空闲") : "未知";
                        return new Response.SpotInfo(
                                p != null ? p.getParkingNo() : "", p != null ? p.getArea() : "",
                                p != null ? (p.getParkingType() == 1 ? "地上" : "地下") : "", statusText);
                    })
                    .toList();
        }

        return new Response(freeCount, soldCount, rentedCount, freeSpots, mySpots);
    }
}
