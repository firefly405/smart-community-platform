package com.smartcommunity.service;

import com.smartcommunity.entity.BaseBuilding;
import com.smartcommunity.entity.BaseHouse;
import com.smartcommunity.entity.BaseOwner;
import com.smartcommunity.entity.BaseUnit;
import com.smartcommunity.entity.GuardAccess;
import com.smartcommunity.entity.GuardAccessLog;
import com.smartcommunity.entity.GuardVisitor;
import com.smartcommunity.entity.SysUser;
import com.smartcommunity.mapper.BaseBuildingMapper;
import com.smartcommunity.mapper.BaseHouseMapper;
import com.smartcommunity.mapper.BaseOwnerMapper;
import com.smartcommunity.mapper.BaseUnitMapper;
import com.smartcommunity.mapper.GuardAccessMapper;
import com.smartcommunity.mapper.GuardVisitorMapper;
import com.smartcommunity.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 批量填充业主姓名、房号、楼栋/单元/门禁名称等展示字段。
 */
@Service
@RequiredArgsConstructor
public class BizDisplayService {

    private final BaseOwnerMapper ownerMapper;
    private final BaseHouseMapper houseMapper;
    private final BaseBuildingMapper buildingMapper;
    private final BaseUnitMapper unitMapper;
    private final GuardAccessMapper accessMapper;
    private final GuardVisitorMapper visitorMapper;
    private final SysUserMapper userMapper;

    public Map<Long, String> ownerNameMap(Collection<Long> ownerIds) {
        Set<Long> ids = normalizeIds(ownerIds);
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<BaseOwner> owners = ownerMapper.selectBatchIds(ids);
        Map<Long, String> map = new HashMap<>();
        for (BaseOwner o : owners) {
            if (o != null && o.getId() != null) {
                map.put(o.getId(), o.getRealName());
            }
        }
        return map;
    }

    public Map<Long, String> houseNoMap(Collection<Long> houseIds) {
        Set<Long> ids = normalizeIds(houseIds);
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<BaseHouse> houses = houseMapper.selectBatchIds(ids);
        Map<Long, String> map = new HashMap<>();
        for (BaseHouse h : houses) {
            if (h != null && h.getId() != null) {
                map.put(h.getId(), h.getHouseNo());
            }
        }
        return map;
    }

    public Map<Long, String> buildingNameMap(Collection<Long> buildingIds) {
        Set<Long> ids = normalizeIds(buildingIds);
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<BaseBuilding> list = buildingMapper.selectBatchIds(ids);
        Map<Long, String> map = new HashMap<>();
        for (BaseBuilding b : list) {
            if (b != null && b.getId() != null) {
                String name = b.getBuildingName() != null && !b.getBuildingName().isBlank()
                        ? b.getBuildingName() : b.getBuildingNo();
                map.put(b.getId(), name);
            }
        }
        return map;
    }

    public Map<Long, String> unitNoMap(Collection<Long> unitIds) {
        Set<Long> ids = normalizeIds(unitIds);
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<BaseUnit> list = unitMapper.selectBatchIds(ids);
        Map<Long, String> map = new HashMap<>();
        for (BaseUnit u : list) {
            if (u != null && u.getId() != null) {
                map.put(u.getId(), u.getUnitNo());
            }
        }
        return map;
    }

    public Map<Long, String> accessNameMap(Collection<Long> accessIds) {
        Set<Long> ids = normalizeIds(accessIds);
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<GuardAccess> list = accessMapper.selectBatchIds(ids);
        Map<Long, String> map = new HashMap<>();
        for (GuardAccess a : list) {
            if (a != null && a.getId() != null) {
                map.put(a.getId(), a.getAccessName());
            }
        }
        return map;
    }

    public <T> void fill(List<T> records,
                         Function<T, Long> ownerIdGetter,
                         Function<T, Long> houseIdGetter,
                         BiConsumer<T, String> ownerNameSetter,
                         BiConsumer<T, String> houseNoSetter) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Map<Long, String> owners = ownerNameMap(records.stream()
                .map(ownerIdGetter)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        Map<Long, String> houses = houseNoMap(records.stream()
                .map(houseIdGetter)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        for (T r : records) {
            Long oid = ownerIdGetter.apply(r);
            Long hid = houseIdGetter.apply(r);
            if (ownerNameSetter != null && oid != null) {
                ownerNameSetter.accept(r, owners.get(oid));
            }
            if (houseNoSetter != null && hid != null) {
                houseNoSetter.accept(r, houses.get(hid));
            }
        }
    }

    public <T> void fillOwnerOnly(List<T> records,
                                  Function<T, Long> ownerIdGetter,
                                  BiConsumer<T, String> ownerNameSetter) {
        fill(records, ownerIdGetter, t -> null, ownerNameSetter, null);
    }

    /** 填充房屋楼栋/单元/业主展示字段 */
    public void fillHouses(List<BaseHouse> houses) {
        if (houses == null || houses.isEmpty()) {
            return;
        }
        Map<Long, String> buildings = buildingNameMap(houses.stream()
                .map(BaseHouse::getBuildingId).filter(Objects::nonNull).collect(Collectors.toSet()));
        Map<Long, String> units = unitNoMap(houses.stream()
                .map(BaseHouse::getUnitId).filter(Objects::nonNull).collect(Collectors.toSet()));
        Map<Long, String> owners = ownerNameMap(houses.stream()
                .map(BaseHouse::getOwnerId).filter(Objects::nonNull).collect(Collectors.toSet()));
        for (BaseHouse h : houses) {
            if (h.getBuildingId() != null) {
                h.setBuildingName(buildings.get(h.getBuildingId()));
            }
            if (h.getUnitId() != null) {
                h.setUnitName(units.get(h.getUnitId()));
            }
            if (h.getOwnerId() != null) {
                h.setOwnerName(owners.get(h.getOwnerId()));
            }
        }
    }

    /** 填充通行记录设备名/用户名 */
    public void fillAccessLogs(List<GuardAccessLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return;
        }
        Map<Long, String> devices = accessNameMap(logs.stream()
                .map(GuardAccessLog::getAccessId).filter(Objects::nonNull).collect(Collectors.toSet()));

        Set<Long> visitorIds = new HashSet<>();
        Set<Long> userIds = new HashSet<>();
        for (GuardAccessLog log : logs) {
            if (log.getUserId() == null || log.getUserId() <= 0) {
                continue;
            }
            if (log.getUserType() != null && log.getUserType() == 4) {
                visitorIds.add(log.getUserId());
            } else {
                userIds.add(log.getUserId());
            }
        }
        Map<Long, String> visitors = new HashMap<>();
        if (!visitorIds.isEmpty()) {
            List<GuardVisitor> list = visitorMapper.selectBatchIds(visitorIds);
            for (GuardVisitor v : list) {
                if (v != null && v.getId() != null) {
                    visitors.put(v.getId(), v.getVisitorName());
                }
            }
        }
        Map<Long, String> users = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<SysUser> list = userMapper.selectBatchIds(userIds);
            for (SysUser u : list) {
                if (u != null && u.getId() != null) {
                    String name = u.getRealName() != null && !u.getRealName().isBlank()
                            ? u.getRealName() : u.getUsername();
                    users.put(u.getId(), name);
                }
            }
        }

        for (GuardAccessLog log : logs) {
            if (log.getAccessId() != null && log.getAccessId() > 0) {
                log.setAccessName(devices.get(log.getAccessId()));
            } else {
                log.setAccessName("未指定设备");
            }
            if (log.getUserId() != null && log.getUserId() > 0) {
                if (log.getUserType() != null && log.getUserType() == 4) {
                    log.setUserName(visitors.get(log.getUserId()));
                } else {
                    log.setUserName(users.get(log.getUserId()));
                }
            }
            if ((log.getUserName() == null || log.getUserName().isBlank())
                    && log.getRemark() != null && log.getRemark().startsWith("访客核销: ")) {
                String r = log.getRemark().substring("访客核销: ".length());
                int slash = r.indexOf(" / ");
                log.setUserName(slash > 0 ? r.substring(0, slash) : r);
            }
        }
    }

    private Set<Long> normalizeIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> set = new HashSet<>();
        for (Long id : ids) {
            if (id != null && id > 0) {
                set.add(id);
            }
        }
        return set;
    }
}
