package com.smartcommunity.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartcommunity.entity.BaseTenant;

public interface BaseTenantService extends IService<BaseTenant> {
    java.util.List<BaseTenant> getByHouseId(Long houseId);
}