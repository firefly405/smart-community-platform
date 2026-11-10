package com.smartcommunity.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartcommunity.entity.GuardAccess;
import com.smartcommunity.mapper.GuardAccessMapper;
import com.smartcommunity.service.GuardAccessService;
import org.springframework.stereotype.Service;

@Service
public class GuardAccessServiceImpl extends ServiceImpl<GuardAccessMapper, GuardAccess> implements GuardAccessService {
}