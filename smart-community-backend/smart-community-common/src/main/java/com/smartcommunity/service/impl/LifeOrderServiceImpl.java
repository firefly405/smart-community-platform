package com.smartcommunity.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartcommunity.entity.LifeOrder;
import com.smartcommunity.mapper.LifeOrderMapper;
import com.smartcommunity.service.LifeOrderService;
import org.springframework.stereotype.Service;

@Service
public class LifeOrderServiceImpl extends ServiceImpl<LifeOrderMapper, LifeOrder> implements LifeOrderService {
}