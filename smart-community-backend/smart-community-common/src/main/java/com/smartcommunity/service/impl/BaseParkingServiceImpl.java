package com.smartcommunity.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartcommunity.entity.BaseParking;
import com.smartcommunity.mapper.BaseParkingMapper;
import com.smartcommunity.service.BaseParkingService;
import org.springframework.stereotype.Service;

@Service
public class BaseParkingServiceImpl extends ServiceImpl<BaseParkingMapper, BaseParking> implements BaseParkingService {
}