package com.smartcommunity.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartcommunity.entity.BaseVehicle;

public interface BaseVehicleService extends IService<BaseVehicle> {
    void createWithParking(BaseVehicle vehicle);
    void updateWithParking(BaseVehicle vehicle);
    void deleteWithParking(Long id);
}
