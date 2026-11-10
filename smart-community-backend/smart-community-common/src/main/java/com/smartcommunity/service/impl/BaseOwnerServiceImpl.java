package com.smartcommunity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartcommunity.common.exception.BusinessException;
import com.smartcommunity.entity.BaseHouse;
import com.smartcommunity.entity.BaseOwner;
import com.smartcommunity.entity.BaseOwnerHouse;
import com.smartcommunity.mapper.BaseHouseMapper;
import com.smartcommunity.mapper.BaseOwnerHouseMapper;
import com.smartcommunity.mapper.BaseOwnerMapper;
import com.smartcommunity.service.BaseOwnerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BaseOwnerServiceImpl extends ServiceImpl<BaseOwnerMapper, BaseOwner> implements BaseOwnerService {

    private final BaseOwnerHouseMapper ownerHouseMapper;
    private final BaseHouseMapper houseMapper;

    @Override
    public List<Long> getHouseIds(Long ownerId) {
        return ownerHouseMapper.selectHouseIdsByOwnerId(ownerId);
    }

    @Override
    @Transactional
    public void bindHouses(Long ownerId, List<Long> houseIds) {
        if (getById(ownerId) == null) {
            throw new BusinessException(404, "业主不存在");
        }

        // 原绑定房屋 ID（用于解绑后清空 house.owner_id）
        List<Long> oldHouseIds = ownerHouseMapper.selectHouseIdsByOwnerId(ownerId);
        Set<Long> newHouseIdSet = houseIds == null ? Set.of() : new HashSet<>(houseIds);

        ownerHouseMapper.deleteByOwnerId(ownerId);

        if (houseIds != null && !houseIds.isEmpty()) {
            List<BaseOwnerHouse> list = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            for (Long houseId : houseIds) {
                BaseOwnerHouse oh = new BaseOwnerHouse();
                oh.setOwnerId(ownerId);
                oh.setHouseId(houseId);
                oh.setRelationType(1);
                oh.setCreateTime(now);
                list.add(oh);
            }
            ownerHouseMapper.insertBatch(list);

            // 同步：新绑定房屋写入 owner_id
            houseMapper.update(null, new LambdaUpdateWrapper<BaseHouse>()
                    .in(BaseHouse::getId, houseIds)
                    .set(BaseHouse::getOwnerId, ownerId));
        }

        // 同步：解绑房屋若仍指向本业主则清空
        if (oldHouseIds != null) {
            List<Long> unbound = oldHouseIds.stream()
                    .filter(id -> !newHouseIdSet.contains(id))
                    .collect(Collectors.toList());
            if (!unbound.isEmpty()) {
                houseMapper.update(null, new LambdaUpdateWrapper<BaseHouse>()
                        .in(BaseHouse::getId, unbound)
                        .eq(BaseHouse::getOwnerId, ownerId)
                        .set(BaseHouse::getOwnerId, null));
            }
        }
    }

    @Override
    public List<BaseOwner> getByHouseId(Long houseId) {
        List<Long> ownerIds = ownerHouseMapper.selectList(
                new LambdaQueryWrapper<BaseOwnerHouse>()
                        .eq(BaseOwnerHouse::getHouseId, houseId)
        ).stream().map(BaseOwnerHouse::getOwnerId).collect(Collectors.toList());
        if (ownerIds.isEmpty()) return List.of();
        return listByIds(ownerIds);
    }
}
