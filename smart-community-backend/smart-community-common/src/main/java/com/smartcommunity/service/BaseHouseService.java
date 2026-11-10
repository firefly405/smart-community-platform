package com.smartcommunity.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartcommunity.entity.BaseHouse;

public interface BaseHouseService extends IService<BaseHouse> {
    java.util.List<BaseHouse> getByBuildingId(Long buildingId);
    java.util.List<BaseHouse> getByOwnerId(Long ownerId);
}