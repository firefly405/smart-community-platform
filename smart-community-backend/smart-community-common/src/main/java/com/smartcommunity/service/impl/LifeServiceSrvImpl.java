package com.smartcommunity.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartcommunity.entity.LifeService;
import com.smartcommunity.mapper.LifeServiceMapper;
import com.smartcommunity.service.LifeServiceSrv;
import org.springframework.stereotype.Service;

@Service
public class LifeServiceSrvImpl extends ServiceImpl<LifeServiceMapper, LifeService> implements LifeServiceSrv {
}