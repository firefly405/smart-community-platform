package com.smartcommunity.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartcommunity.entity.BaseUnit;
import com.smartcommunity.mapper.BaseUnitMapper;
import com.smartcommunity.service.BaseUnitService;
import org.springframework.stereotype.Service;

@Service
public class BaseUnitServiceImpl extends ServiceImpl<BaseUnitMapper, BaseUnit> implements BaseUnitService {
}