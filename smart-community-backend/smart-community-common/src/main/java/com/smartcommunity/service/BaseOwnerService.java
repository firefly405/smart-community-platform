package com.smartcommunity.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartcommunity.entity.BaseOwner;

import java.util.List;

public interface BaseOwnerService extends IService<BaseOwner> {

    List<Long> getHouseIds(Long ownerId);

    void bindHouses(Long ownerId, List<Long> houseIds);

    List<BaseOwner> getByHouseId(Long houseId);
}