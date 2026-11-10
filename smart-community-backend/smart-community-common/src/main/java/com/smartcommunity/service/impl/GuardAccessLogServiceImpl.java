package com.smartcommunity.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartcommunity.entity.GuardAccessLog;
import com.smartcommunity.mapper.GuardAccessLogMapper;
import com.smartcommunity.service.GuardAccessLogService;
import org.springframework.stereotype.Service;

@Service
public class GuardAccessLogServiceImpl extends ServiceImpl<GuardAccessLogMapper, GuardAccessLog> implements GuardAccessLogService {
}