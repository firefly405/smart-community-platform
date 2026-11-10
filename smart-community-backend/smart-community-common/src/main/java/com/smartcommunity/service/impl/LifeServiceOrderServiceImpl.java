package com.smartcommunity.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartcommunity.entity.LifeServiceOrder;
import com.smartcommunity.mapper.LifeServiceOrderMapper;
import com.smartcommunity.service.LifeServiceOrderService;
import org.springframework.stereotype.Service;

@Service
public class LifeServiceOrderServiceImpl extends ServiceImpl<LifeServiceOrderMapper, LifeServiceOrder> implements LifeServiceOrderService {
}