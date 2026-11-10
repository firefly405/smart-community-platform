package com.smartcommunity.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartcommunity.entity.LifeProduct;
import com.smartcommunity.mapper.LifeProductMapper;
import com.smartcommunity.service.LifeProductService;
import org.springframework.stereotype.Service;

@Service
public class LifeProductServiceImpl extends ServiceImpl<LifeProductMapper, LifeProduct> implements LifeProductService {
}