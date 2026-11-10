package com.smartcommunity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartcommunity.entity.BaseHouse;
import com.smartcommunity.mapper.BaseHouseMapper;
import com.smartcommunity.service.BaseHouseService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BaseHouseServiceImpl extends ServiceImpl<BaseHouseMapper, BaseHouse> implements BaseHouseService {

    @Override
    public List<BaseHouse> getByBuildingId(Long buildingId) {
        return list(new LambdaQueryWrapper<BaseHouse>().eq(BaseHouse::getBuildingId, buildingId));
    }

    @Override
    public List<BaseHouse> getByOwnerId(Long ownerId) {
        return list(new LambdaQueryWrapper<BaseHouse>().eq(BaseHouse::getOwnerId, ownerId));
    }
}