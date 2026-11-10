package com.smartcommunity.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartcommunity.entity.BaseFamily;

public interface BaseFamilyService extends IService<BaseFamily> {
    java.util.List<BaseFamily> getByOwnerId(Long ownerId);
}