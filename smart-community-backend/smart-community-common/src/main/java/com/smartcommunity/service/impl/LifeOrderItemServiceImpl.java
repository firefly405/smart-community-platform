package com.smartcommunity.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartcommunity.entity.LifeOrderItem;
import com.smartcommunity.mapper.LifeOrderItemMapper;
import com.smartcommunity.service.LifeOrderItemService;
import org.springframework.stereotype.Service;

@Service
public class LifeOrderItemServiceImpl extends ServiceImpl<LifeOrderItemMapper, LifeOrderItem> implements LifeOrderItemService {
}
