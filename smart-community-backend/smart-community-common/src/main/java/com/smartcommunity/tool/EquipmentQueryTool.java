package com.smartcommunity.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.smartcommunity.entity.PropEquipment;
import com.smartcommunity.entity.PropEquipmentMaintain;
import com.smartcommunity.mapper.PropEquipmentMapper;
import com.smartcommunity.mapper.PropEquipmentMaintainMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
@Description("查询小区设施设备状态：电梯、水泵、消防、门禁、监控等设备的运行状态和维保记录")
public class EquipmentQueryTool implements Function<EquipmentQueryTool.Request, EquipmentQueryTool.Response> {

    private final PropEquipmentMapper equipmentMapper;
    private final PropEquipmentMaintainMapper maintainMapper;

    public record Request(
            @JsonPropertyDescription("设备类型：ELEVATOR=电梯, PUMP=水泵, FIRE=消防, CAMERA=监控, ACCESS=门禁, ALL=全部")
            String equipmentType,
            @JsonPropertyDescription("状态筛选：NORMAL=正常, MAINTENANCE=维修中, STOPPED=停用, ALL=全部")
            String statusFilter) {}

    public record Response(int count, List<EquipInfo> equipments) {
        public record EquipInfo(String name, String type, String status, String location,
                                String supplier, String supplierPhone, String lastMaintainDate) {}
    }

    @Override
    public Response apply(Request req) {
        LambdaQueryWrapper<PropEquipment> qw = new LambdaQueryWrapper<>();
        String et = req.equipmentType();
        String sf = req.statusFilter();
        if (et != null && !"ALL".equals(et)) {
            int t;
            if ("ELEVATOR".equals(et)) { t = 1; }
            else if ("PUMP".equals(et)) { t = 2; }
            else if ("FIRE".equals(et)) { t = 3; }
            else if ("CAMERA".equals(et)) { t = 4; }
            else if ("ACCESS".equals(et)) { t = 5; }
            else { t = -1; }
            if (t >= 0) qw.eq(PropEquipment::getEquipmentType, t);
        }
        if (sf != null && !"ALL".equals(sf)) {
            int s;
            if ("NORMAL".equals(sf)) { s = 1; }
            else if ("MAINTENANCE".equals(sf)) { s = 2; }
            else if ("STOPPED".equals(sf)) { s = 0; }
            else { s = -1; }
            if (s >= 0) qw.eq(PropEquipment::getStatus, s);
        }
        qw.orderByDesc(PropEquipment::getStatus);
        List<PropEquipment> list = equipmentMapper.selectList(qw);
        List<Response.EquipInfo> ei = list.stream().map(e -> {
            int equipTypeVal = e.getEquipmentType() != null ? e.getEquipmentType() : 1;
            String tt;
            if (equipTypeVal == 1) { tt = "电梯"; }
            else if (equipTypeVal == 2) { tt = "水泵"; }
            else if (equipTypeVal == 3) { tt = "消防"; }
            else if (equipTypeVal == 4) { tt = "监控"; }
            else if (equipTypeVal == 5) { tt = "门禁"; }
            else if (equipTypeVal == 6) { tt = "其他"; }
            else { tt = "其他"; }
            int equipStatusVal = e.getStatus() != null ? e.getStatus() : 0;
            String st;
            if (equipStatusVal == 0) { st = "停用"; }
            else if (equipStatusVal == 1) { st = "正常"; }
            else if (equipStatusVal == 2) { st = "维修中"; }
            else if (equipStatusVal == 3) { st = "报废"; }
            else { st = "未知"; }
            List<PropEquipmentMaintain> ms = maintainMapper.selectList(new LambdaQueryWrapper<PropEquipmentMaintain>().eq(PropEquipmentMaintain::getEquipmentId, e.getId()).orderByDesc(PropEquipmentMaintain::getMaintainDate).last("LIMIT 1"));
            String lm = ms.isEmpty() ? "暂无记录" : ms.get(0).getMaintainDate().toString();
            return new Response.EquipInfo(e.getEquipmentName(), tt, st, e.getLocation(), e.getSupplier(), e.getSupplierPhone(), lm);
        }).toList();
        return new Response(ei.size(), ei);
    }
}
