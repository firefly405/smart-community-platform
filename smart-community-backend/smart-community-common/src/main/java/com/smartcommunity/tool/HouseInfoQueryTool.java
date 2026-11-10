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
@Description("查询业主的房屋信息：地址、面积、户型、家庭成员、租客信息")
public class HouseInfoQueryTool implements Function<HouseInfoQueryTool.Request, HouseInfoQueryTool.Response> {

    private final BaseHouseMapper houseMapper;
    private final BaseOwnerMapper ownerMapper;
    private final BaseOwnerHouseMapper ownerHouseMapper;
    private final BaseFamilyMapper familyMapper;
    private final BaseTenantMapper tenantMapper;
    private final BaseBuildingMapper buildingMapper;
    private final BaseUnitMapper unitMapper;

    public record Request(
            @JsonProperty(required = true)
            @JsonPropertyDescription("业主ID")
            Long ownerId) {}

    public record Response(String ownerName, String ownerPhone, List<HouseDetail> houses,
                           List<FamilyMember> families, List<TenantInfo> tenants) {
        public record HouseDetail(Long id, String address, String houseType, String area, String orientation, int floor, String status) {}
        public record FamilyMember(String name, String relation, String gender) {}
        public record TenantInfo(String name, String phone, String leaseEndDate) {}
    }

    @Override
    public Response apply(Request req) {
        Long ownerId = req.ownerId();
        BaseOwner owner = ownerMapper.selectById(ownerId);
        List<BaseOwnerHouse> ohList = ownerHouseMapper.selectList(new LambdaQueryWrapper<BaseOwnerHouse>().eq(BaseOwnerHouse::getOwnerId, ownerId));
        List<Long> houseIds = ohList.stream().map(BaseOwnerHouse::getHouseId).toList();

        List<BaseHouse> houses = houseMapper.selectBatchIds(houseIds);
        List<Response.HouseDetail> hd = houses.stream().map(h -> {
            BaseBuilding b = buildingMapper.selectById(h.getBuildingId());
            BaseUnit u = unitMapper.selectById(h.getUnitId());
            String addr = (b != null ? b.getBuildingName() : "") + " " + (u != null ? u.getUnitNo() + "单元" : "") + " " + h.getHouseNo() + "室";
            int houseStatusVal = h.getStatus() != null ? h.getStatus() : 0;
            String st;
            if (houseStatusVal == 0) { st = "空置"; }
            else if (houseStatusVal == 1) { st = "已售"; }
            else if (houseStatusVal == 2) { st = "已租"; }
            else if (houseStatusVal == 3) { st = "自住"; }
            else { st = "未知"; }
            return new Response.HouseDetail(h.getId(), addr, h.getHouseType(), h.getArea() != null ? h.getArea().toString() : "", h.getOrientation(), h.getFloor(), st);
        }).toList();

        List<BaseFamily> families = familyMapper.selectList(new LambdaQueryWrapper<BaseFamily>().eq(BaseFamily::getOwnerId, ownerId).eq(BaseFamily::getStatus, 1));
        List<Response.FamilyMember> fm = families.stream().map(f -> new Response.FamilyMember(f.getRealName(), f.getRelation(), f.getGender() == 1 ? "男" : "女")).toList();

        List<BaseTenant> tenants = tenantMapper.selectList(new LambdaQueryWrapper<BaseTenant>().eq(BaseTenant::getOwnerId, ownerId).eq(BaseTenant::getStatus, 1));
        List<Response.TenantInfo> ti = tenants.stream().map(t -> new Response.TenantInfo(t.getRealName(), t.getPhone(), t.getLeaseEndDate() != null ? t.getLeaseEndDate().toString() : "")).toList();

        return new Response(owner != null ? owner.getRealName() : "", owner != null ? owner.getPhone() : "", hd, fm, ti);
    }
}
