package com.smartcommunity.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartcommunity.entity.GuardVisitor;
import com.smartcommunity.mapper.GuardVisitorMapper;
import com.smartcommunity.service.GuardVisitorService;
import org.springframework.stereotype.Service;

@Service
public class GuardVisitorServiceImpl extends ServiceImpl<GuardVisitorMapper, GuardVisitor> implements GuardVisitorService {
}