package com.smartcommunity.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartcommunity.entity.LifeActivity;
import com.smartcommunity.mapper.LifeActivityMapper;
import com.smartcommunity.service.LifeActivityService;
import org.springframework.stereotype.Service;

@Service
public class LifeActivityServiceImpl extends ServiceImpl<LifeActivityMapper, LifeActivity> implements LifeActivityService {
}