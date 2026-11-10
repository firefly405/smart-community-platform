package com.smartcommunity.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartcommunity.entity.GuardAlarm;
import com.smartcommunity.mapper.GuardAlarmMapper;
import com.smartcommunity.service.GuardAlarmService;
import org.springframework.stereotype.Service;

@Service
public class GuardAlarmServiceImpl extends ServiceImpl<GuardAlarmMapper, GuardAlarm> implements GuardAlarmService {
}