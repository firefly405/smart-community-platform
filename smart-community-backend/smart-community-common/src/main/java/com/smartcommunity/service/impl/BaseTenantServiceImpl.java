package com.smartcommunity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartcommunity.entity.BaseTenant;
import com.smartcommunity.mapper.BaseTenantMapper;
import com.smartcommunity.service.BaseTenantService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BaseTenantServiceImpl extends ServiceImpl<BaseTenantMapper, BaseTenant> implements BaseTenantService {

    @Override
    public List<BaseTenant> getByHouseId(Long houseId) {
        return list(new LambdaQueryWrapper<BaseTenant>().eq(BaseTenant::getHouseId, houseId));
    }
}