package com.smartcommunity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartcommunity.entity.BaseFamily;
import com.smartcommunity.mapper.BaseFamilyMapper;
import com.smartcommunity.service.BaseFamilyService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BaseFamilyServiceImpl extends ServiceImpl<BaseFamilyMapper, BaseFamily> implements BaseFamilyService {

    @Override
    public List<BaseFamily> getByOwnerId(Long ownerId) {
        return list(new LambdaQueryWrapper<BaseFamily>().eq(BaseFamily::getOwnerId, ownerId));
    }
}