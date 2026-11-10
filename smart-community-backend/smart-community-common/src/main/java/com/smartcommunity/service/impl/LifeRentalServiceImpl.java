package com.smartcommunity.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartcommunity.entity.LifeRental;
import com.smartcommunity.mapper.LifeRentalMapper;
import com.smartcommunity.service.LifeRentalService;
import org.springframework.stereotype.Service;

@Service
public class LifeRentalServiceImpl extends ServiceImpl<LifeRentalMapper, LifeRental> implements LifeRentalService {
}